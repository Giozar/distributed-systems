package com.giozar04.bootstrap;

import com.giozar04.configs.DatabaseConfig;
import com.giozar04.databases.domain.interfaces.DatabaseConnectionInterface;
import com.giozar04.databases.infrastructure.repositories.DatabaseConnectionMySQL;
import com.giozar04.shared.logging.CustomLogger;

public class DatabaseInitializer {
    private final DatabaseConfig dbConfig;
    private final CustomLogger logger;

    public DatabaseInitializer(DatabaseConfig dbConfig, CustomLogger logger) {
        this.dbConfig = dbConfig;
        this.logger = logger;
    }

    public DatabaseConnectionInterface initialize() {
        DatabaseConnectionInterface connection = DatabaseConnectionMySQL.getInstance(
                dbConfig.getHost(),
                dbConfig.getPort(),
                dbConfig.getName(),
                dbConfig.getUsername(),
                dbConfig.getPassword()
        );
        logger.info("Conexión a la base de datos establecida exitosamente.");
        return connection;
    }
}
