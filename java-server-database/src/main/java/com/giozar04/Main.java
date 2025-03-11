package com.giozar04;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.giozar04.databases.domain.interfaces.DatabaseConnectionInterface;
import com.giozar04.databases.infrastructure.MySQLDatabaseConnection;
import com.giozar04.servers.application.services.ServerService;
import com.giozar04.servers.domain.exceptions.ServerOperationException;
import com.giozar04.shared.logging.CustomLogger;
import com.giozar04.transactions.application.services.TransactionService;
import com.giozar04.transactions.domain.interfaces.TransactionRepositoryInterface;
import com.giozar04.transactions.infrastructure.repositories.TransactionRepositoryMySQL;

/**
 * Punto de entrada principal de la aplicación.
 * Inicializa y configura todos los componentes necesarios.
 */
public class Main {

    private static final CustomLogger LOGGER = new CustomLogger();
    
    // Configuración del servidor
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;
    
    // Configuración de la base de datos
    private static final String DATABASE_HOST = "127.0.0.1";
    private static final String DATABASE_PORT = "3306";
    private static final String DATABASE_NAME = "finanzas";
    private static final String DATABASE_USERNAME = "giovanni";
    private static final String DATABASE_PASSWORD = "finanzas123";

    /**
     * Método principal para iniciar la aplicación.
     *
     * @param args Argumentos de la línea de comandos.
     */
    public static void main(String[] args) {
        LOGGER.info("Iniciando aplicación...");
        
        ExecutorService threadPool = null;
        DatabaseConnectionInterface dbConnection = null;
        ServerService server = null;
        
        try {
            // Inicializar el pool de hilos para el servidor
            threadPool = Executors.newCachedThreadPool();
            
            // Obtener la conexión a la base de datos (Singleton)
            dbConnection = MySQLDatabaseConnection.getInstance(
                    DATABASE_HOST, DATABASE_PORT, DATABASE_NAME, 
                    DATABASE_USERNAME, DATABASE_PASSWORD);
            
            LOGGER.info("Conexión a la base de datos establecida exitosamente.");
            
            // Inicializar los repositorios
            TransactionRepositoryInterface transactionRepository = 
                    new TransactionRepositoryMySQL(dbConnection);
            
            // Inicializar los servicios con sus respectivos repositorios
            TransactionService transactionService = new TransactionService(transactionRepository);
            
            LOGGER.info("Servicios inicializados correctamente.");
            
            // Obtener la instancia del servidor (Singleton) e inyectar dependencias
            server = ServerService.getInstance(SERVER_HOST, SERVER_PORT, threadPool, LOGGER);
            
            // Configurar los manejadores del servidor con los servicios necesarios
            setupServerHandlers(server, transactionService);
            
            // Iniciar el servidor
            server.startServer();
            LOGGER.info("Servidor iniciado correctamente en " + SERVER_HOST + ":" + SERVER_PORT);
            
            // Mantener el servidor en ejecución
            keepServerRunning(server);
            
        } catch (ServerOperationException | IOException e) {
            LOGGER.error("Error al iniciar la aplicación", e);
            
            // Intento de limpieza en caso de error
            try {
                if (server != null && server.isServerRunning()) {
                    server.stopServer();
                }
            } catch (ServerOperationException ex) {
                LOGGER.error("Error al detener el servidor durante manejo de excepciones", ex);
            }
            
            System.exit(1);
        }
    }
    
    /**
     * Configura los manejadores específicos del servidor.
     *
     * @param server El servidor a configurar.
     * @param transactionService El servicio de transacciones para manejar operaciones relacionadas.
     */
    private static void setupServerHandlers(ServerService server, TransactionService transactionService) {
        // Aquí configurarías los manejadores de mensajes específicos
        // Ejemplo: registrar callbacks, asignar controladores, etc.
        
        // Por ejemplo, podrías tener algo como:
        // server.registerHandler("CREATE_TRANSACTION", (clientSocket, message) -> {
        //     // Lógica para manejar creación de transacciones
        //     Transaction transaction = parseTransaction(message);
        //     Transaction created = transactionService.createTransaction(transaction);
        //     sendResponse(clientSocket, created);
        // });
        
        LOGGER.info("Manejadores del servidor configurados");
    }
    
    /**
     * Mantiene el servidor en ejecución y maneja el apagado ordenado.
     *
     * @param server El servidor a mantener en ejecución.
     */
    private static void keepServerRunning(ServerService server) {
        // Agregar gancho de apagado para limpieza al terminar
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Apagando servidor...");
            try {
                if (server != null && server.isServerRunning()) {
                    server.stopServer();
                }
            } catch (ServerOperationException e) {
                LOGGER.error("Error al apagar el servidor", e);
            }
        }));
        
        // Aquí puedes implementar lógica adicional para mantener la aplicación en ejecución
        // Por ejemplo, un bucle o una espera en un objeto de sincronización
        
        // Ejemplo simple: Mantener el hilo principal activo con un bucle sencillo
        LOGGER.info("Servidor en ejecución. Presiona Ctrl+C para detener.");
        try {
            // Este objeto se usa solo para mantener el programa en ejecución
            final Object lock = new Object();
            synchronized (lock) {
                lock.wait();
            }
        } catch (InterruptedException e) {
            LOGGER.info("Aplicación interrumpida. Finalizando...");
            Thread.currentThread().interrupt();
        }
    }
}