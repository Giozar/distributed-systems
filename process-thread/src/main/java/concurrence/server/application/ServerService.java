package concurrence.server.application;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import concurrence.server.domain.Client;
import concurrence.server.domain.ClientRepository;
import concurrence.server.domain.ServerExceptions;
import concurrence.shared.Logger;

import java.net.Socket;

public class ServerService {

    private static ServerService instance;

    private final int port;
    private final ClientRepository clientRepository;
    private final Logger logger;
    private ServerSocket serverSocket;
    private boolean running;
    private final ExecutorService threadPool;

    private ServerService(int port, ClientRepository clientRepository, Logger logger) {
        this.port = port;
        this.clientRepository = clientRepository;
        this.logger = logger;
        this.threadPool = Executors.newCachedThreadPool();
    }

    public static synchronized ServerService getInstance(int port, ClientRepository clientRepository, Logger logger) {

        if( instance == null) {
            synchronized (ServerService.class) {
                if( instance == null ) {
                    instance = new ServerService(port, clientRepository, logger); 
                }
            }
        }

        return instance;
    }

    public static ServerService getInstance() {
        return instance;
    }

    private void handleClient ( Client client) {
        try {
            Socket socket = client.getSocket();

            var outputStream = socket.getOutputStream();
            var dataOutputStream = new java.io.DataOutputStream(outputStream);
            var inputStream = socket.getInputStream();
            var inputStreamReader = new java.io.InputStreamReader(inputStream);
            var bufferedReader = new java.io.BufferedReader(inputStreamReader);

            dataOutputStream.writeUTF("Conectado al servidor. Tu ID es: " + client.getId());

            String message;
            

            while( (message = bufferedReader.readLine()) != null){
                logger.info("Mensaje recibido del cliente " + client.getId() + ": " + message);

                dataOutputStream.writeUTF("Mensaje recibido: " + message);

                if(message.equalsIgnoreCase(("exit"))){
                    break;
                }
            }

            logger.info("Cliente " + client.getId() + " desconectado");
            clientRepository.removeClient(client.getId());
            socket.close();
        } catch (IOException e) {
            clientRepository.removeClient(client.getId());
            logger.error("Error al manejar cliente " + client.getId(), e);
            throw new ServerExceptions.ClientHandlingException("Error al manejar el cliente", e);
        }
    }

    private void acceptConnections(){
        while( running ) {
            try {
                Socket clientSocket = serverSocket.accept();
                int clientId = clientRepository.getClientCount() + 1;
                Client client = new Client(clientSocket, clientId);
                clientRepository.addClient(client);
                logger.info("Cliente conectado:" + clientId + ". Total de clientes: " + clientRepository.getClientCount());
                threadPool.execute(() -> handleClient(client));
            } catch (IOException e) {
                if( running) {
                    logger.error("Error al aceptar conexión de cliente", e);
                }
            }
        }
    }

    public void stopt() {
        running = false;
        threadPool.shutdown();

        try {
            if(serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            logger.info("Servidor detenido");

            synchronized (ServerService.class) {
                instance = null;
            }
        } catch (IOException e) {
            logger.error("Error al cerrar el socket del servidor", e);
        }
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            logger.info("Server started on port " + port);

            new Thread(this::acceptConnections).start();
        } catch( IOException e) {
            throw new ServerExceptions.SeverStartException("Error al iniciar el servidor en el puerto " + port , e);
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getClientCount() {
        return clientRepository.getClientCount();
    }

}
