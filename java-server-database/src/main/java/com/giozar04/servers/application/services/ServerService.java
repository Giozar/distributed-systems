package com.giozar04.servers.application.services;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.giozar04.servers.domain.exceptions.ServerOperationException;
import com.giozar04.servers.domain.handlers.MessageHandler;
import com.giozar04.servers.domain.models.ClientConnection;
import com.giozar04.servers.domain.models.Message;
import com.giozar04.servers.domain.models.ServerAbstract;
import com.giozar04.shared.logging.CustomLogger;

/**
 * Implementación concreta del servidor de sockets como Singleton.
 * Garantiza que solo exista una instancia del servidor en toda la aplicación.
 * Incluye manejo automático de recursos con ShutdownHook y procesamiento de mensajes.
 */
public class ServerService extends ServerAbstract {

    private static volatile ServerService instance;
    
    // Mapa para almacenar los clientes conectados
    private final Map<Integer, ClientConnection> connectedClients;
    
    // Mapa para almacenar los manejadores de mensajes según su tipo
    private final Map<String, MessageHandler> messageHandlers;
    
    // Flag para verificar si el ShutdownHook ya se ha registrado
    private boolean shutdownHookRegistered = false;
    
    /**
     * Constructor privado para implementar el patrón Singleton.
     */
    private ServerService(String serverHost, int serverPort, ExecutorService threadPool, CustomLogger logger) {
        super(serverHost, serverPort, threadPool, logger);
        this.connectedClients = new ConcurrentHashMap<>();
        this.messageHandlers = new ConcurrentHashMap<>();
        registerShutdownHook();
    }

    /**
     * Obtiene la instancia única del servidor, creándola si es necesario.
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
     * Variante de getInstance utilizando un logger por defecto.
     */
    public static ServerService getInstance(String serverHost, int serverPort, ExecutorService threadPool) {
        return getInstance(serverHost, serverPort, threadPool, new CustomLogger());
    }

    /**
     * Retorna la instancia ya creada o null si aún no se ha inicializado.
     */
    public static ServerService getInstance() {
        return instance;
    }

    /**
     * Registra un manejador para un tipo específico de mensaje.
     */
    public ServerService registerHandler(String messageType, MessageHandler handler) {
        messageHandlers.put(messageType, handler);
        logger.info("Manejador registrado para mensajes de tipo: " + messageType);
        return this;
    }
    
    /**
     * Elimina el manejador de un tipo específico de mensaje.
     */
    public ServerService unregisterHandler(String messageType) {
        messageHandlers.remove(messageType);
        logger.info("Manejador eliminado para mensajes de tipo: " + messageType);
        return this;
    }

    /**
     * Registra un ShutdownHook para liberar recursos al finalizar la aplicación.
     */
    private void registerShutdownHook() {
        if (!shutdownHookRegistered) {
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Apagando el servidor y liberando recursos...");
                try {
                    if (isServerRunning()) {
                        stopServer();
                    }
                    if (threadPool != null && !threadPool.isShutdown()) {
                        threadPool.shutdown();
                        if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                            threadPool.shutdownNow();
                            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                                logger.error("El pool de hilos no se cerró correctamente");
                            }
                        }
                    }
                } catch (ServerOperationException | InterruptedException e) {
                    logger.error("Error durante la liberación de recursos del servidor", e);
                    Thread.currentThread().interrupt();
                }
                logger.info("Servidor detenido y recursos liberados correctamente");
            }));
            shutdownHookRegistered = true;
            logger.info("ShutdownHook registrado para la limpieza de recursos");
        }
    }

    @Override
    public void startServer() throws ServerOperationException, IOException {
        baseStartServer();
        // Lanza un hilo que acepta conexiones de forma continua
        threadPool.submit(() -> {
            try {
                while (isRunning) {
                    acceptClientConnections();
                }
            } catch (ServerOperationException | IOException e) {
                logger.error("Error en el ciclo de aceptación de conexiones: ", e);
            }
        });
        logger.info("Servidor listo para aceptar conexiones entrantes");
    }

    @Override
    public void stopServer() throws ServerOperationException {
        // Cierra las conexiones de cada cliente conectado
        connectedClients.values().forEach(client -> {
            try {
                Socket socket = client.getSocket();
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                logger.warn("Error al cerrar la conexión del cliente " + client.getId(), e);
            }
        });
        connectedClients.clear();
        baseStopServer();
    }

    @Override
    public void restartServer() throws ServerOperationException, IOException {
        logger.info("Reiniciando el servidor...");
        stopServer();
        startServer();
        logger.info("Servidor reiniciado correctamente");
    }

    @Override
    public boolean isServerRunning() {
        return isRunning && serverSocket != null && !serverSocket.isClosed();
    }

    /**
     * Maneja la aceptación de una conexión entrante.
     * Este método acepta una conexión y delega su procesamiento a handleClientConnection.
     */
    @Override
    public void acceptClientConnections() throws ServerOperationException, IOException {
        if (!isServerRunning() || serverSocket == null || serverSocket.isClosed()) {
            throw new ServerOperationException("El servidor no está en ejecución");
        }
        try {
            logger.info("Esperando nueva conexión de cliente...");
            Socket socket = serverSocket.accept();
            // Se utiliza el contador heredado para asignar un ID único
            int clientId = clientIdGenerator.incrementAndGet();
            ClientConnection clientConnection = new ClientConnection(socket, clientId);
            // Delegar el manejo de la conexión
            handleClientConnection(clientConnection);
        } catch (IOException e) {
            if (isServerRunning()) {
                logger.error("Error al aceptar la conexión de un cliente", e);
                throw e;
            } else {
                logger.info("El servidor fue detenido mientras se esperaba una conexión");
            }
        }
    }

    @Override
    public int getConnectedClientsCount() {
        return connectedClients.size();
    }

    /**
     * Maneja la conexión de un cliente.
     * Se registra el cliente y se procesa la comunicación en un hilo separado.
     */
    @Override
    public void handleClientConnection(ClientConnection clientConnection) throws ServerOperationException {
        if (clientConnection == null) {
            logger.warn("Se recibió una conexión nula, ignorando");
            return;
        }
        // Registrar el cliente en el mapa de conectados
        connectedClients.put(clientConnection.getId(), clientConnection);
        logger.info("Cliente " + clientConnection.getId() + " conectado desde " +
                   clientConnection.getSocket().getInetAddress().getHostAddress());

        threadPool.submit(() -> {
            ObjectInputStream in = null;
            ObjectOutputStream out = null;
            try {
                Socket socket = clientConnection.getSocket();
                // Configurar streams para comunicación de objetos
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                
                // Enviar mensaje de bienvenida
                Message welcomeMessage = Message.createSuccessMessage("WELCOME",
                        "Conexión establecida. Cliente ID: " + clientConnection.getId());
                out.writeObject(welcomeMessage);
                out.flush();
                
                // Bucle de comunicación con el cliente
                while (!socket.isClosed() && isRunning) {
                    Object receivedObj = in.readObject();
                    if (receivedObj instanceof Message) {
                        Message receivedMessage = (Message) receivedObj;
                        logger.info("Mensaje recibido del cliente " + clientConnection.getId() +
                                   ": " + receivedMessage.getType());
                        processMessage(clientConnection, receivedMessage, out);
                    } else {
                        logger.warn("Mensaje recibido no es del tipo esperado: " +
                                    (receivedObj != null ? receivedObj.getClass().getName() : "null"));
                        Message errorMessage = Message.createErrorMessage("ERROR", "Tipo de mensaje no soportado");
                        out.writeObject(errorMessage);
                        out.flush();
                    }
                }
            } catch (ClassNotFoundException e) {
                logger.error("Error de serialización al procesar mensaje del cliente " + clientConnection.getId(), e);
            } catch (IOException e) {
                logger.info("Cliente " + clientConnection.getId() + " desconectado: " + e.getMessage());
            } catch (Exception e) {
                logger.error("Error inesperado al manejar el cliente " + clientConnection.getId(), e);
            } finally {
                // Liberar recursos y eliminar al cliente del registro
                try {
                    if (in != null) in.close();
                    if (out != null) out.close();
                    if (!clientConnection.getSocket().isClosed()) {
                        clientConnection.getSocket().close();
                    }
                } catch (IOException e) {
                    logger.error("Error al cerrar recursos del cliente " + clientConnection.getId(), e);
                }
                connectedClients.remove(clientConnection.getId());
                logger.info("Cliente " + clientConnection.getId() + " eliminado del registro");
            }
        });
    }

    /**
     * Procesa un mensaje recibido utilizando el manejador correspondiente.
     */
    private void processMessage(ClientConnection clientConnection, Message message, ObjectOutputStream out) throws IOException {
        String messageType = message.getType();
        MessageHandler handler = messageHandlers.get(messageType);
        
        if (handler != null) {
            try {
                Message response = handler.handleMessage(clientConnection, message);
                if (response != null) {
                    out.writeObject(response);
                    out.flush();
                    logger.info("Respuesta enviada al cliente " + clientConnection.getId() +
                               " para mensaje tipo: " + messageType);
                }
            } catch (Exception e) {
                logger.error("Error al procesar mensaje tipo '" + messageType +
                           "' del cliente " + clientConnection.getId(), e);
                Message errorResponse = Message.createErrorMessage(messageType, "Error al procesar solicitud: " + e.getMessage());
                out.writeObject(errorResponse);
                out.flush();
            }
        } else {
            logger.warn("No hay manejador registrado para mensajes de tipo: " + messageType);
            Message errorResponse = Message.createErrorMessage(messageType, "Tipo de mensaje no soportado: " + messageType);
            out.writeObject(errorResponse);
            out.flush();
        }
    }

    /**
     * Envía un mensaje a un cliente específico.
     */
    public void sendMessageToClient(int clientId, Message message)
            throws IOException, ServerOperationException {
        ClientConnection clientConnection = connectedClients.get(clientId);
        if (clientConnection == null) {
            throw new ServerOperationException("Cliente con ID " + clientId + " no encontrado");
        }
        try {
            Socket socket = clientConnection.getSocket();
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            out.writeObject(message);
            out.flush();
            logger.info("Mensaje enviado al cliente " + clientId + ": " + message.getType());
        } catch (IOException e) {
            logger.error("Error al enviar mensaje al cliente " + clientId, e);
            throw e;
        }
    }
    
    /**
     * Envía un mensaje a todos los clientes conectados.
     */
    public void broadcastMessage(Message message) {
        connectedClients.values().forEach(clientConnection -> {
            try {
                Socket socket = clientConnection.getSocket();
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                logger.error("Error al enviar mensaje de broadcast al cliente " + clientConnection.getId(), e);
            }
        });
        logger.info("Mensaje de broadcast enviado a " + connectedClients.size() +
                  " clientes: " + message.getType());
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
