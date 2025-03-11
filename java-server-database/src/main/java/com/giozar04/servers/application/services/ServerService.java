package com.giozar04.servers.application.services;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.giozar04.servers.domain.classes.AbstractServer;
import com.giozar04.servers.domain.exceptions.ServerOperationException;
import com.giozar04.servers.domain.handlers.MessageHandler;
import com.giozar04.servers.domain.models.ClientSocket;
import com.giozar04.servers.domain.models.Message;
import com.giozar04.shared.logging.CustomLogger;

/**
 * Implementación concreta del servidor de sockets como Singleton.
 * Garantiza que solo exista una instancia del servidor en toda la aplicación.
 * Incluye manejo automático de recursos con ShutdownHook y procesamiento de mensajes.
 */
public class ServerService extends AbstractServer {

    // Instancia única del Singleton
    private static volatile ServerService instance;
    
    // Mapa para almacenar todos los clientes conectados
    private final Map<Integer, ClientSocket> connectedClients;
    
    // Contador atómico para generar IDs únicos para cada cliente
    private final AtomicInteger clientIdCounter;
    
    // Mapa para almacenar los manejadores de mensajes según su tipo
    private final Map<String, MessageHandler> messageHandlers;
    
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
        this.messageHandlers = new ConcurrentHashMap<>();
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
     * Registra un manejador para un tipo específico de mensaje.
     *
     * @param messageType El tipo de mensaje que el manejador procesará.
     * @param handler El manejador que procesará el mensaje.
     * @return La instancia actual del servidor (para encadenamiento de métodos).
     */
    public ServerService registerHandler(String messageType, MessageHandler handler) {
        messageHandlers.put(messageType, handler);
        logger.info("Manejador registrado para mensajes de tipo: " + messageType);
        return this;
    }
    
    /**
     * Elimina un manejador para un tipo específico de mensaje.
     *
     * @param messageType El tipo de mensaje cuyo manejador se eliminará.
     * @return La instancia actual del servidor (para encadenamiento de métodos).
     */
    public ServerService unregisterHandler(String messageType) {
        messageHandlers.remove(messageType);
        logger.info("Manejador eliminado para mensajes de tipo: " + messageType);
        return this;
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
            ObjectInputStream in = null;
            ObjectOutputStream out = null;
            
            try {
                Socket socket = clientSocket.getSocket();
                
                // Configurar streams para comunicación de objetos
                out = new ObjectOutputStream(socket.getOutputStream());
                in = new ObjectInputStream(socket.getInputStream());
                
                // Enviar mensaje de bienvenida
                Message welcomeMessage = Message.createSuccessMessage(
                        "WELCOME", "Conexión establecida. Cliente ID: " + clientSocket.getId());
                out.writeObject(welcomeMessage);
                out.flush();
                
                // Bucle de comunicación con el cliente
                while (!socket.isClosed() && isRunning) {
                    // Intentar leer un mensaje
                    Object receivedObj = in.readObject();
                    
                    if (receivedObj instanceof Message) {
                        Message receivedMessage = (Message) receivedObj;
                        logger.info("Mensaje recibido del cliente " + clientSocket.getId() + 
                                   ": " + receivedMessage.getType());
                        
                        // Procesar el mensaje con el manejador apropiado
                        processMessage(clientSocket, receivedMessage, out);
                    } else {
                        logger.warn("Mensaje recibido no es del tipo esperado: " + 
                                    (receivedObj != null ? receivedObj.getClass().getName() : "null"));
                        
                        // Enviar mensaje de error
                        Message errorMessage = Message.createErrorMessage(
                                "ERROR", "Tipo de mensaje no soportado");
                        out.writeObject(errorMessage);
                        out.flush();
                    }
                }
            } catch (ClassNotFoundException e) {
                logger.error("Error de serialización al procesar mensaje del cliente " + clientSocket.getId(), e);
            } catch (IOException e) {
                // Esto ocurre normalmente cuando el cliente se desconecta
                logger.info("Cliente " + clientSocket.getId() + " desconectado: " + e.getMessage());
            } catch (Exception e) {
                logger.error("Error inesperado al manejar el cliente " + clientSocket.getId(), e);
            } finally {
                // Cerrar recursos
                try {
                    if (in != null) in.close();
                    if (out != null) out.close();
                    
                    if (!clientSocket.getSocket().isClosed()) {
                        clientSocket.getSocket().close();
                    }
                } catch (IOException e) {
                    logger.error("Error al cerrar recursos del cliente " + clientSocket.getId(), e);
                }
                
                // Eliminar cliente del mapa
                connectedClients.remove(clientSocket.getId());
                logger.info("Cliente " + clientSocket.getId() + " eliminado del registro");
            }
        });
    }

    /**
     * Procesa un mensaje recibido utilizando el manejador apropiado.
     *
     * @param clientSocket El socket del cliente que envió el mensaje.
     * @param message El mensaje recibido.
     * @param out El stream de salida para enviar respuestas.
     * @throws IOException Si ocurre un error al enviar la respuesta.
     */
    private void processMessage(ClientSocket clientSocket, Message message, 
                               ObjectOutputStream out) throws IOException {
        String messageType = message.getType();
        
        // Buscar el manejador adecuado para este tipo de mensaje
        MessageHandler handler = messageHandlers.get(messageType);
        
        if (handler != null) {
            try {
                // Procesar el mensaje con el manejador y obtener la respuesta
                Message response = handler.handleMessage(clientSocket, message);
                
                // Enviar la respuesta si no es null
                if (response != null && out != null) {
                    out.writeObject(response);
                    out.flush();
                    logger.info("Respuesta enviada al cliente " + clientSocket.getId() + 
                               " para mensaje tipo: " + messageType);
                }
            } catch (Exception e) {
                logger.error("Error al procesar mensaje tipo '" + messageType + 
                           "' del cliente " + clientSocket.getId(), e);
                
                // Enviar mensaje de error al cliente
                if (out != null) {
                    Message errorResponse = Message.createErrorMessage(
                        messageType, "Error al procesar solicitud: " + e.getMessage());
                    out.writeObject(errorResponse);
                    out.flush();
                }
            }
        } else {
            logger.warn("No hay manejador registrado para mensajes de tipo: " + messageType);
            
            // Enviar mensaje de error al cliente
            if (out != null) {
                Message errorResponse = Message.createErrorMessage(
                    messageType, "Tipo de mensaje no soportado: " + messageType);
                out.writeObject(errorResponse);
                out.flush();
            }
        }
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
    
    /**
     * Envía un mensaje a un cliente específico.
     *
     * @param clientId El ID del cliente.
     * @param message El mensaje a enviar.
     * @throws IOException Si ocurre un error de E/S durante el envío.
     * @throws ServerOperationException Si el cliente no está conectado.
     */
    public void sendMessageToClient(int clientId, Message message) 
            throws IOException, ServerOperationException {
        ClientSocket clientSocket = connectedClients.get(clientId);
        
        if (clientSocket == null) {
            throw new ServerOperationException("Cliente con ID " + clientId + " no encontrado");
        }
        
        try {
            Socket socket = clientSocket.getSocket();
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
     *
     * @param message El mensaje a enviar.
     */
    public void broadcastMessage(Message message) {
        for (ClientSocket clientSocket : connectedClients.values()) {
            try {
                Socket socket = clientSocket.getSocket();
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                logger.error("Error al enviar mensaje de broadcast al cliente " + 
                           clientSocket.getId(), e);
            }
        }
        logger.info("Mensaje broadcast enviado a " + connectedClients.size() + 
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