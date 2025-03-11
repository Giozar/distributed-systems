package com.giozar04;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.giozar04.databases.domain.interfaces.DatabaseConnectionInterface;
import com.giozar04.databases.infrastructure.MySQLDatabaseConnection;
import com.giozar04.servers.application.services.ServerService;
import com.giozar04.shared.logging.CustomLogger;
import com.giozar04.transactions.application.services.TransactionService;
import com.giozar04.transactions.domain.interfaces.TransactionRepositoryInterface;
import com.giozar04.transactions.infrastructure.handlers.TransactionHandlers;
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
    private static final String DATABASE_HOST = "localhost";
    private static final String DATABASE_PORT = "3306";
    private static final String DATABASE_NAME = "transactions";
    private static final String DATABASE_USERNAME = "XXXX";
    private static final String DATABASE_PASSWORD = "XXXX";

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
            
        } catch (Exception e) {
            LOGGER.error("Error al iniciar la aplicación", e);
            
            // Intento de limpieza en caso de error
            try {
                if (server != null && server.isServerRunning()) {
                    server.stopServer();
                }
            } catch (Exception ex) {
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
        // Registrar manejadores para operaciones de transacciones
        server.registerHandler(
            TransactionHandlers.MessageTypes.CREATE_TRANSACTION,
            TransactionHandlers.createTransactionHandler(transactionService)
        );
        
        server.registerHandler(
            TransactionHandlers.MessageTypes.GET_TRANSACTION,
            TransactionHandlers.getTransactionHandler(transactionService)
        );
        
        server.registerHandler(
            TransactionHandlers.MessageTypes.UPDATE_TRANSACTION,
            TransactionHandlers.updateTransactionHandler(transactionService)
        );
        
        server.registerHandler(
            TransactionHandlers.MessageTypes.DELETE_TRANSACTION,
            TransactionHandlers.deleteTransactionHandler(transactionService)
        );
        
        server.registerHandler(
            TransactionHandlers.MessageTypes.GET_ALL_TRANSACTIONS,
            TransactionHandlers.getAllTransactionsHandler(transactionService)
        );
        
        LOGGER.info("Manejadores de transacciones registrados en el servidor");
    }
    
    /**
     * Mantiene el servidor en ejecución y maneja el apagado ordenado.
     *
     * @param server El servidor a mantener en ejecución.
     */
    private static void keepServerRunning(ServerService server) {
        // El ShutdownHook ya está registrado internamente en el ServerService,
        // por lo que aquí solo necesitamos mantener el hilo principal activo
        
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