package com.giozar04.bootstrap;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.giozar04.configs.ServerConfig;
import com.giozar04.servers.application.services.ServerService;
import com.giozar04.servers.domain.exceptions.ServerOperationException;
import com.giozar04.shared.logging.CustomLogger;
import com.giozar04.transactions.application.services.TransactionService;
import com.giozar04.transactions.infrastructure.handlers.TransactionHandlers;

public class ServerInitializer {
    private final ServerConfig serverConfig;
    private final CustomLogger logger;
    private final ExecutorService threadPool;

    public ServerInitializer(ServerConfig serverConfig, CustomLogger logger) {
        this.serverConfig = serverConfig;
        this.logger = logger;
        this.threadPool = Executors.newCachedThreadPool();
    }

    public ServerService initialize(TransactionService transactionService)
            throws ServerOperationException, IOException {
        ServerService server = ServerService.getInstance(
                serverConfig.getHost(),
                serverConfig.getPort(),
                threadPool,
                logger
        );
        // Configurar los handlers del servidor
        server.registerHandler(
                TransactionHandlers.TransactionMessageTypes.CREATE_TRANSACTION,
                TransactionHandlers.createTransactionHandler(transactionService)
        );
        server.registerHandler(
                TransactionHandlers.TransactionMessageTypes.GET_TRANSACTION,
                TransactionHandlers.getTransactionHandler(transactionService)
        );
        server.registerHandler(
                TransactionHandlers.TransactionMessageTypes.UPDATE_TRANSACTION,
                TransactionHandlers.updateTransactionHandler(transactionService)
        );
        server.registerHandler(
                TransactionHandlers.TransactionMessageTypes.DELETE_TRANSACTION,
                TransactionHandlers.deleteTransactionHandler(transactionService)
        );
        server.registerHandler(
                TransactionHandlers.TransactionMessageTypes.GET_ALL_TRANSACTIONS,
                TransactionHandlers.getAllTransactionsHandler(transactionService)
        );

        logger.info("Manejadores de transacciones registrados en el servidor");
        return server;
    }
}
