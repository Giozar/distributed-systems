package com.giozar04.servers.application.services;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.giozar04.servers.domain.classes.AbstractServer;
import com.giozar04.servers.domain.exceptions.ServerOperationException;
import com.giozar04.servers.domain.models.ClientSocket;
import com.giozar04.shared.logging.CustomLogger;

/**
 * Implementación concreta del servidor de sockets como Singleton.
 * Garantiza que solo exista una instancia del servidor en toda la aplicación.
 * Incluye manejo automático de recursos con ShutdownHook.
 */
public class ServerService extends AbstractServer {

    // Instancia única del Singleton
    private static volatile ServerService instance;
    
    // Mapa para almacenar todos los clientes conectados
    private final Map<Integer, ClientSocket> connectedClients;
    
    // Contador atómico para generar IDs únicos para cada cliente
    private final AtomicInteger clientIdCounter;
    
    // Flag para verificar si el ShutdownHook ya se ha registrado
    private boolean shutdownHookRegistered = false;
    
    /**
     * Constructor privado para implementar Singleton.
     * Inicializa el servidor con la configuración suministrada.
     *
     * @param serverHost La dirección del host donde se ejecutará el servidor.
     * @param serverPort El puerto en el que escuchará el servidor.
     * @param threadPool El pool de hilos para manejar conexiones de clientes.
     * @param logger El logger para registrar eventos del servidor.
     */
    private ServerService(String serverHost, int serverPort, ExecutorService threadPool, CustomLogger logger) {
        super(serverHost, serverPort, threadPool, logger);
        this.connectedClients = new ConcurrentHashMap<>();
        this.clientIdCounter = new AtomicInteger(1);
        registerShutdownHook();
    }

    /**
     * Obtiene la instancia única del servidor.
     * Si no existe, la crea con la configuración suministrada.
     *
     * @param serverHost La dirección del host donde se ejecutará el servidor.
     * @param serverPort El puerto en el que escuchará el servidor.
     * @param threadPool El pool de hilos para manejar conexiones de clientes.
     * @param logger El logger para registrar eventos del servidor.
     * @return La instancia única del servidor.
     */
    public static ServerService getInstance(String serverHost, int serverPort, ExecutorService threadPool, CustomLogger logger) {
        if (instance == null) {
            synchronized (ServerService.class) {
                if (instance == null) {
                    instance = new ServerService(serverHost, serverPort, threadPool, logger);
                }
            }
        }
        return instance;
    }

    /**
     * Obtiene la instancia única del servidor con un logger por defecto.
     *
     * @param serverHost La dirección del host donde se ejecutará el servidor.
     * @param serverPort El puerto en el que escuchará el servidor.
     * @param threadPool El pool de hilos para manejar conexiones de clientes.
     * @return La instancia única del servidor.
     */
    public static ServerService getInstance(String serverHost, int serverPort, ExecutorService threadPool) {
        return getInstance(serverHost, serverPort, threadPool, new CustomLogger());
    }

    /**
     * Obtiene la instancia existente del servidor.
     * 
     * @return La instancia del servidor o null si aún no se ha inicializado.
     */
    public static ServerService getInstance() {
        return instance;
    }

    /**
     * Registra un gancho de apagado para liberar recursos al cerrar la aplicación.
     */
    private void registerShutdownHook() {
        if (!shutdownHookRegistered) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Apagando servidor y liberando recursos...");
                try {
                    if (isServerRunning()) {
                        stopServer();
                    }
                    
                    if (threadPool != null && !threadPool.isShutdown()) {
                        threadPool.shutdown();
                        try {
                            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                                threadPool.shutdownNow();
                                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                                    logger.error("El pool de hilos no se cerró correctamente");
                                }
                            }
                        } catch (InterruptedException e) {
                            threadPool.shutdownNow();
                            Thread.currentThread().interrupt();
                        }
                    }
                    
                } catch (ServerOperationException e) {
                    logger.error("Error durante la liberación de recursos del servidor", e);
                }
                logger.info("Servidor detenido y recursos liberados correctamente");
            }));
            shutdownHookRegistered = true;
            logger.info("ShutdownHook registrado para limpieza de recursos");
        }
    }

    @Override
    public void startServer() throws ServerOperationException, IOException {
        baseStartServer();
        
        // Iniciar hilo para aceptar conexiones
        threadPool.submit(() -> {
            try {
                while (isRunning) {
                    acceptClientSocketConnections();
                }
            } catch (ServerOperationException | IOException e) {
                logger.error("Error en el bucle de aceptación de conexiones", e);
            }
        });
        
        logger.info("Servidor listo para aceptar conexiones");
    }

    @Override
    public void stopServer() throws ServerOperationException {
        // Cerrar todas las conexiones de clientes antes de detener el servidor
        for (ClientSocket client : connectedClients.values()) {
            try {
                Socket socket = client.getSocket();
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                logger.warn("Error al cerrar la conexión del cliente " + client.getId(), e);
            }
        }
        
        connectedClients.clear();
        baseStopServer();
    }

    @Override
    public void restartServer() throws ServerOperationException, IOException {
        logger.info("Reiniciando servidor...");
        stopServer();
        startServer();
        logger.info("Servidor reiniciado correctamente");
    }

    @Override
    public boolean isServerRunning() {
        return isRunning && serverSocket != null && !serverSocket.isClosed();
    }

    @Override
    public void handleClientSocket(ClientSocket clientSocket) throws ServerOperationException {
        if (clientSocket == null) {
            logger.warn("Se recibió un clientSocket nulo, ignorando");
            return;
        }
        
        // Registrar el cliente en el mapa de clientes conectados
        connectedClients.put(clientSocket.getId(), clientSocket);
        logger.info("Cliente " + clientSocket.getId() + " conectado desde " + 
                   clientSocket.getSocket().getInetAddress().getHostAddress());

        // Manejar comunicación con el cliente en un hilo separado
        threadPool.submit(() -> {
            try {
                // Aquí iría la lógica específica para comunicarse con el cliente
                // Por ejemplo, leer/escribir mensajes, procesar solicitudes, etc.
                
                // Por ahora solo mantenemos la conexión abierta
                Socket socket = clientSocket.getSocket();
                while (!socket.isClosed() && isRunning) {
                    // Implementar lógica de comunicación específica aquí
                    Thread.sleep(1000); // Para evitar consumo excesivo de CPU en este ejemplo
                }
            } catch (InterruptedException e) {
                logger.error("Error al manejar el cliente " + clientSocket.getId(), e);
            } finally {
                try {
                    // Cerrar recursos y eliminar cliente del mapa cuando se desconecta
                    if (!clientSocket.getSocket().isClosed()) {
                        clientSocket.getSocket().close();
                    }
                } catch (IOException e) {
                    logger.error("Error al cerrar el socket del cliente " + clientSocket.getId(), e);
                }
                
                connectedClients.remove(clientSocket.getId());
                logger.info("Cliente " + clientSocket.getId() + " desconectado");
            }
        });
    }

    @Override
    public void acceptClientSocketConnections() throws ServerOperationException, IOException {
        if (!isRunning || serverSocket == null || serverSocket.isClosed()) {
            throw new ServerOperationException("El servidor no está en ejecución");
        }
        
        try {
            logger.info("Esperando nueva conexión de cliente...");
            Socket clientSocketConnection = serverSocket.accept();
            
            // Crear objeto ClientSocket con ID único
            int clientId = clientIdCounter.getAndIncrement();
            ClientSocket clientSocket = new ClientSocket(clientSocketConnection, clientId);
            
            // Manejar la conexión del cliente
            handleClientSocket(clientSocket);
        } catch (IOException e) {
            if (isRunning) {
                logger.error("Error al aceptar conexión de cliente", e);
                throw e;
            } else {
                // Si el servidor está deteniéndose, ignoramos esta excepción
                logger.info("Servidor detenido mientras esperaba conexiones");
            }
        }
    }

    @Override
    public int getConnectedClientSocketsCount() {
        return connectedClients.size();
    }
    
    /**
     * Obtiene un cliente conectado por su ID.
     *
     * @param clientId El ID del cliente.
     * @return El ClientSocket con el ID especificado, o null si no existe.
     */
    public ClientSocket getClientById(int clientId) {
        return connectedClients.get(clientId);
    }
    
    @Override
    public void close() throws Exception {
        try {
            stopServer();
        } catch (ServerOperationException e) {
            logger.error("Error al cerrar el servidor", e);
            throw e;
        }
    }
}