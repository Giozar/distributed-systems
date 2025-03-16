package com.giozar04.bootstrap;

import java.io.IOException;

import com.giozar04.configs.DatabaseConfig;
import com.giozar04.configs.ServerConfig;
import com.giozar04.databases.domain.interfaces.DatabaseConnectionInterface;
import com.giozar04.servers.application.services.ServerService;
import com.giozar04.servers.domain.exceptions.ServerOperationException;
import com.giozar04.shared.logging.CustomLogger;
import com.giozar04.transactions.application.services.TransactionService;
import com.giozar04.transactions.domain.interfaces.TransactionRepositoryInterface;
import com.giozar04.transactions.infrastructure.repositories.TransactionRepositoryMySQL;

public class ApplicationInitializer {
    private final CustomLogger logger = new CustomLogger();

    public void start() {
        logger.info("Iniciando aplicación...");

        DatabaseConfig databaseConfig = new DatabaseConfig();
        ServerConfig serverConfig = new ServerConfig();

        DatabaseInitializer dbInitializer = new DatabaseInitializer(databaseConfig, logger);
        DatabaseConnectionInterface dbConnection = dbInitializer.initialize();

        // Inicializar repositorios y servicios de transacciones
        TransactionRepositoryInterface transactionRepository =
                new TransactionRepositoryMySQL(dbConnection);
        TransactionService transactionService =
                new TransactionService(transactionRepository);
        logger.info("Servicios inicializados correctamente.");

        ServerInitializer serverInitializer = new ServerInitializer(serverConfig, logger);
        try {
            ServerService server = serverInitializer.initialize(transactionService);
            server.startServer();
            logger.info("Servidor iniciado correctamente en " + serverConfig.getHost() + ":" + serverConfig.getPort());
            
            // Mantener el servidor en ejecución
            keepServerRunning();
        } catch (ServerOperationException | IOException e) {
            logger.error("Error al iniciar la aplicación", e);
            System.exit(1);
        }
    }

    private void keepServerRunning() {
        logger.info("Servidor en ejecución. Presiona Ctrl+C para detener.");
        final Object lock = new Object();
        try {
            synchronized (lock) {
                lock.wait();
            }
        } catch (InterruptedException e) {
            logger.info("Aplicación interrumpida. Finalizando...");
            Thread.currentThread().interrupt();
        }
    }
}
