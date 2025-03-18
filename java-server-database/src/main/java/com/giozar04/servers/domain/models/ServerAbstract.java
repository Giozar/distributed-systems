package com.giozar04.servers.domain.models;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import com.giozar04.servers.domain.exceptions.ServerOperationException;
import com.giozar04.servers.domain.interfaces.ServerInterface;
import com.giozar04.shared.logging.CustomLogger;

/**
 * Implementación base para servidores de sockets.
 * Proporciona funcionalidad común y estructura para implementaciones específicas.
 */
public abstract class ServerAbstract implements ServerInterface {

    protected static final ReentrantLock LOCK = new ReentrantLock();
    protected ServerSocket serverSocket;
    protected final String serverHost;
    protected final int serverPort;
    protected volatile boolean isRunning;
    protected final ExecutorService threadPool;
    protected final CustomLogger logger;
    protected final AtomicInteger connectedClientsCount = new AtomicInteger(0);
    // Generador único de identificadores para clientes
    protected final AtomicInteger clientIdGenerator = new AtomicInteger(0);

    /**
     * Constructor para inicializar un servidor abstracto.
     *
     * @param serverHost La dirección del host donde se ejecutará el servidor.
     * @param serverPort El puerto en el que escuchará el servidor.
     * @param threadPool El pool de hilos para manejar conexiones de clientes.
     * @param logger El logger para registrar eventos del servidor.
     * @throws NullPointerException si algún parámetro requerido es nulo.
     * @throws IllegalArgumentException si el puerto está fuera del rango válido.
     */
    protected ServerAbstract(String serverHost, int serverPort, ExecutorService threadPool, CustomLogger logger) {
        this.serverHost = Objects.requireNonNull(serverHost, "El serverHost no puede ser nulo");
        if (serverPort < 0 || serverPort > 65535) {
            throw new IllegalArgumentException("El puerto debe estar entre 0 y 65535");
        }
        this.serverPort = serverPort;
        this.threadPool = Objects.requireNonNull(threadPool, "El threadPool no puede ser nulo");
        this.logger = Objects.requireNonNull(logger, "El logger no puede ser nulo");
        this.isRunning = false;
    }

    /**
     * Constructor alternativo que crea un logger por defecto.
     *
     * @param serverHost La dirección del host donde se ejecutará el servidor.
     * @param serverPort El puerto en el que escuchará el servidor.
     * @param threadPool El pool de hilos para manejar conexiones de clientes.
     */
    protected ServerAbstract(String serverHost, int serverPort, ExecutorService threadPool) {
        this(serverHost, serverPort, threadPool, new CustomLogger());
    }

    /**
     * Método base para iniciar el servidor.
     *
     * @throws ServerOperationException si ocurre un error al iniciar el servidor.
     * @throws IOException si ocurre un error de E/S durante la operación.
     */
    protected void baseStartServer() throws ServerOperationException, IOException {
        LOCK.lock();
        try {
            if (isRunning) {
                logger.info("El servidor ya se encuentra activo en " + serverHost + ":" + serverPort);
                return;
            }
            InetAddress address = InetAddress.getByName(serverHost);
            serverSocket = new ServerSocket(serverPort, 50, address);
            isRunning = true;
            logger.info("Servidor iniciado correctamente en " + serverHost + ":" + serverPort);
        } catch (IOException e) {
            logger.error("Error al iniciar el servidor: " + e.getMessage(), e);
            throw e;
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Método base para detener el servidor.
     *
     * @throws ServerOperationException si ocurre un error al detener el servidor.
     */
    protected void baseStopServer() throws ServerOperationException {
        LOCK.lock();
        try {
            if (!isRunning) {
                logger.info("El servidor ya se encuentra detenido");
                return;
            }
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                    logger.info("Servidor detenido correctamente");
                } catch (IOException e) {
                    logger.error("Error al cerrar el socket del servidor: " + e.getMessage(), e);
                    throw new ServerOperationException("Error al detener el servidor", e);
                }
            }
            isRunning = false;
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Inicia el servidor y lanza en un hilo separado la aceptación de conexiones.
     *
     * @throws ServerOperationException si ocurre un error al iniciar el servidor.
     * @throws IOException si ocurre un error de E/S durante la operación.
     */
    @Override
    public void startServer() throws ServerOperationException, IOException {
        baseStartServer();
        // Inicia la tarea de aceptación de conexiones en un hilo separado
        threadPool.submit(() -> {
            try {
                acceptClientConnections();
            } catch (ServerOperationException | IOException e) {
                logger.error("Error en el ciclo de aceptación de conexiones: " + e.getMessage(), e);
            }
        });
    }

    /**
     * Detiene el servidor.
     *
     * @throws ServerOperationException si ocurre un error al detener el servidor.
     */
    @Override
    public void stopServer() throws ServerOperationException {
        baseStopServer();
    }

    /**
     * Reinicia el servidor (detiene y vuelve a iniciar).
     *
     * @throws ServerOperationException si ocurre un error durante el reinicio.
     * @throws IOException si ocurre un error de E/S durante la operación.
     */
    @Override
    public void restartServer() throws ServerOperationException, IOException {
        stopServer();
        startServer();
    }

    /**
     * Indica si el servidor está en ejecución.
     *
     * @return true si el servidor está ejecutándose, false en caso contrario.
     * @throws ServerOperationException si ocurre un error al obtener el estado.
     */
    @Override
    public boolean isServerRunning() throws ServerOperationException {
        return isRunning;
    }

    /**
     * Acepta conexiones entrantes de clientes en un bucle.
     * Por cada conexión aceptada, se delega el manejo a handleClientConnection en un hilo separado.
     *
     * @throws ServerOperationException si ocurre un error al aceptar conexiones.
     * @throws IOException si ocurre un error de E/S durante la operación.
     */
    @Override
    public void acceptClientConnections() throws ServerOperationException, IOException {
        while (isServerRunning()) {
            try {
                // Asigna un identificador único al cliente
                Socket socket = serverSocket.accept();
                ClientConnection clientConnection = new ClientConnection(socket, clientIdGenerator.incrementAndGet());
                connectedClientsCount.incrementAndGet();
                // Maneja el cliente en un hilo separado
                threadPool.submit(() -> {
                    try {
                        handleClientConnection(clientConnection);
                    } catch (ServerOperationException e) {
                        logger.error("Error al manejar el cliente: " + e.getMessage(), e);
                    } finally {
                        connectedClientsCount.decrementAndGet();
                    }
                });
            } catch (IOException e) {
                // Si el servidor sigue en ejecución, reporta el error
                if (isServerRunning()) {
                    logger.error("Error al aceptar una conexión de cliente: " + e.getMessage(), e);
                }
                throw e;
            }
        }
    }

    /**
     * Retorna el número de clientes actualmente conectados.
     *
     * @return El número de clientes conectados.
     * @throws ServerOperationException si ocurre un error al obtener el conteo.
     */
    @Override
    public int getConnectedClientsCount() throws ServerOperationException {
        return connectedClientsCount.get();
    }

    /**
     * Método abstracto para manejar la conexión de un cliente.
     * Las subclases deben proporcionar la implementación específica.
     *
     * @param clientConnection El cliente a manejar.
     * @throws ServerOperationException si ocurre un error al manejar el cliente.
     */
    @Override
    public abstract void handleClientConnection(ClientConnection clientConnection) throws ServerOperationException;

    /**
     * Cierra los recursos del servidor.
     *
     * @throws Exception si ocurre un error al cerrar los recursos.
     */
    @Override
    public void close() throws Exception {
        try {
            stopServer();
        } catch (ServerOperationException e) {
            logger.error("Error al cerrar el servidor: " + e.getMessage(), e);
            throw e;
        } finally {
            if (threadPool != null && !threadPool.isShutdown()) {
                threadPool.shutdown();
            }
        }
    }
}
