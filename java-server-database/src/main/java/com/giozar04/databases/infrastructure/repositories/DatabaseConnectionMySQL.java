package com.giozar04.databases.infrastructure.repositories;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.giozar04.databases.domain.exceptions.DatabaseExceptions;
import com.giozar04.databases.domain.exceptions.DatabaseExceptions.ConnectionException;
import com.giozar04.databases.domain.exceptions.DatabaseExceptions.DriverException;
import com.giozar04.databases.domain.models.DatabaseConnectionAbstract;

/**
 * Implementación mejorada de conexión a MySQL con manejo de excepciones
 * personalizadas.
 */
public class DatabaseConnectionMySQL extends DatabaseConnectionAbstract {

    // Instancia única (patrón Singleton)
    private static volatile DatabaseConnectionMySQL instance;

    /**
     * Constructor privado que inicializa la conexión con parámetros seguros.
     */
    private DatabaseConnectionMySQL(String databaseHost, String databasePort, String databaseName,
            String databaseUsername, String databasePassword) {
        super(databaseHost, databasePort, databaseName, databaseUsername, databasePassword);
    }

    /**
     * Método estático para obtener la instancia única de la conexión (patrón
     * Singleton).
     *
     * @param databaseHost el host de la base de datos
     * @param databasePort el puerto de la base de datos
     * @param databaseName el nombre de la base de datos
     * @param databaseUsername el nombre de usuario para la conexión
     * @param databasePassword la contraseña para la conexión
     * @return la instancia única de MySQLDatabaseConnection
     */
    public static DatabaseConnectionMySQL getInstance(String databaseHost, String databasePort, String databaseName,
            String databaseUsername, String databasePassword) {
        // Verificación rápida sin bloqueo
        if (instance == null) {
            LOCK.lock();
            try {
                // Verificación doble para garantizar que solo se crea una instancia
                if (instance == null) {
                    instance = new DatabaseConnectionMySQL(databaseHost, databasePort, databaseName, databaseUsername, databasePassword);
                }
            } finally {
                LOCK.unlock();
            }
        }
        return instance;
    }
    
    @Override
protected String buildJdbcUrl() {
    return String.format("jdbc:mysql://%s:%s/%s?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", 
        databaseHost, databasePort, databaseName);
}

@Override
protected void configureConnectionProperties() {
    // Añadir credenciales
    connectionProps.setProperty("user", databaseUsername);
    connectionProps.setProperty("password", databasePassword);

    // Configuración para desarrollo - SSL desactivado para evitar problemas de certificados
    connectionProps.setProperty("useSSL", "false");
    connectionProps.setProperty("allowPublicKeyRetrieval", "true");
    
    // Resto de propiedades
    connectionProps.setProperty("serverTimezone", "UTC");
    connectionProps.setProperty("connectTimeout", String.valueOf(DEFAULT_TIMEOUT * 1000));

    // Configuraciones adicionales para prevenir inyección SQL
    connectionProps.setProperty("allowMultiQueries", "false");

    // Configuración para prevenir fugas de memoria
    connectionProps.setProperty("autoReconnect", "true");
    connectionProps.setProperty("maxReconnects", "3");
}
    @Override
    public void connect() {
        LOCK.lock();
        try {
            if (connection == null || connection.isClosed()) {
                // Cargar el driver explícitamente
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                } catch (ClassNotFoundException e) {
                    logger.error("Driver MySQL no encontrado: " + e.getMessage(), e);
                    throw DriverException.fromClassNotFoundException(e);
                }

                try {
                    // Establecer la conexión usando Properties
                    connection = DriverManager.getConnection(jdbcUrl, connectionProps);

                    // Configurar propiedades adicionales de la conexión
                    connection.setAutoCommit(false); // Control explícito de transacciones

                    logger.info("Conexión MySQL establecida exitosamente con la base de datos");
                } catch (SQLException e) {
                    logger.error("Error al conectar con la base de datos MySQL: " + e.getMessage(), e);
                    throw DatabaseExceptions.translateSQLException(e, "establecer conexión");
                }
            }
        } catch (SQLException e) {
            logger.error("Error inesperado al verificar el estado de la conexión: " + e.getMessage(), e);
            throw new ConnectionException("Error al verificar el estado de la conexión", e);
        } finally {
            LOCK.unlock();
        }
    }

    @Override
    public void disconnect() {
        LOCK.lock();
        try {
            if (connection != null && !connection.isClosed()) {
                try {
                    // Asegurar que todas las transacciones pendientes sean cerradas
                    if (!connection.getAutoCommit()) {
                        connection.rollback();
                    }
                } catch (SQLException e) {
                    logger.warn("Error al hacer rollback de transacciones pendientes: " + e.getMessage(), e);
                    // No lanzamos excepción aquí, continuamos con el cierre
                }

                try {
                    connection.close();
                    connection = null; // Liberar referencia para GC
                    logger.info("Desconexión exitosa de la base de datos MySQL");
                } catch (SQLException e) {
                    logger.error("Error al cerrar la conexión: " + e.getMessage(), e);
                    throw DatabaseExceptions.translateSQLException(e, "cerrar conexión");
                }
            }
        } catch (SQLException e) {
            logger.error("Error inesperado al verificar el estado de la conexión: " + e.getMessage(), e);
            throw new ConnectionException("Error al verificar el estado de la conexión", e);
        } finally {
            LOCK.unlock();
        }
    }

    @Override
    public Connection getConnection() {
        LOCK.lock();
        try {
            if (connection == null || connection.isClosed()) {
                connect();
            }

            // Verificar validez de la conexión
            try {
                if (!isConnectionValid(DEFAULT_TIMEOUT)) {
                    logger.warn("Conexión MySQL inválida, reconectando...", null);
                    disconnect();
                    connect();
                }
            } catch (SQLException e) {
                logger.error("Error al validar la conexión: " + e.getMessage(), e);
                throw new ConnectionException("No se pudo validar la conexión", e);
            }

            return connection;
        } catch (SQLException e) {
            logger.error("Error al verificar el estado de la conexión: " + e.getMessage(), e);
            throw new ConnectionException("Error al verificar el estado de la conexión", e);
        } finally {
            LOCK.unlock();
        }
    }

    @Override
    public void commitTransaction() {
        try {
            if (connection != null && !connection.isClosed() && !connection.getAutoCommit()) {
                try {
                    connection.commit();
                    logger.info("Transacción confirmada exitosamente");
                } catch (SQLException e) {
                    logger.error("Error al confirmar la transacción: " + e.getMessage(), e);
                    throw DatabaseExceptions.translateSQLException(e, "commit de transacción");
                }
            }
        } catch (SQLException e) {
            logger.error("Error al verificar el estado de la conexión: " + e.getMessage(), e);
            throw new ConnectionException("Error al verificar el estado de la conexión", e);
        }
    }

    @Override
    public void rollbackTransaction() {
        try {
            if (connection != null && !connection.isClosed() && !connection.getAutoCommit()) {
                try {
                    connection.rollback();
                    logger.info("Rollback de transacción ejecutado");
                } catch (SQLException e) {
                    logger.error("Error al hacer rollback de la transacción: " + e.getMessage(), e);
                    throw DatabaseExceptions.translateSQLException(e, "rollback de transacción");
                }
            }
        } catch (SQLException e) {
            logger.error("Error al verificar el estado de la conexión: " + e.getMessage(), e);
            throw new ConnectionException("Error al verificar el estado de la conexión", e);
        }
    }

    @Override
    public boolean isConnectionValid(int timeout) throws SQLException {
        if (connection == null) {
            return false;
        }
        return connection.isValid(timeout);
    }
}
