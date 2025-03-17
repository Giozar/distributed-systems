package com.giozar04.servers.domain.models;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
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
    protected String serverHost;
    protected int serverPort;
    protected volatile boolean isRunning;
    protected final ExecutorService threadPool;
    protected final CustomLogger logger;

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
     * Las clases derivadas deben llamar a este método y extender su funcionalidad.
     *
     * @throws ServerOperationException si ocurre un error al iniciar el servidor.
     * @throws IOException si ocurre un error de E/S durante la operación.
     */
    protected void baseStartServer() throws ServerOperationException, IOException {
        LOCK.lock();
        try {
            if (isRunning) {
                logger.info("El servidor ya está en ejecución en " + serverHost + ":" + serverPort);
                return;
            }
            
            InetAddress address = InetAddress.getByName(serverHost);
            serverSocket = new ServerSocket(serverPort, 50, address);
            isRunning = true;
            
            logger.info("Servidor iniciado en " + serverHost + ":" + serverPort);
        } catch (IOException e) {
            logger.error("Error al iniciar el servidor: " + e.getMessage(), e);
            throw e;
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Método base para detener el servidor.
     * Las clases derivadas deben llamar a este método y extender su funcionalidad.
     *
     * @throws ServerOperationException si ocurre un error al detener el servidor.
     */
    protected void baseStopServer() throws ServerOperationException {
        LOCK.lock();
        try {
            if (!isRunning) {
                logger.info("El servidor ya está detenido");
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