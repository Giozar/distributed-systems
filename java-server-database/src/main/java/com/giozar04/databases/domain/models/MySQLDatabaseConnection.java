package com.giozar04.databases.domain.models;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.giozar04.databases.domain.classes.AbstractDatabaseConnection;
import com.giozar04.databases.domain.classes.DatabaseExceptions;
import com.giozar04.databases.domain.classes.DatabaseExceptions.ConnectionException;
import com.giozar04.databases.domain.classes.DatabaseExceptions.DriverException;

/**
 * Implementación mejorada de conexión a MySQL con manejo de excepciones personalizadas.
 */
public class MySQLDatabaseConnection extends AbstractDatabaseConnection {

    // Instancia única (patrón Singleton)
    private static volatile MySQLDatabaseConnection instance;
    
    /**
     * Constructor privado que inicializa la conexión con parámetros seguros.
     */
    private MySQLDatabaseConnection(String host, String port, String databaseName, 
                                   String username, String password) {
        super(host, port, databaseName, username, password);
    }
    
    /**
     * Método estático para obtener la instancia única de la conexión (patrón Singleton).
     */
    public static MySQLDatabaseConnection getInstance(String host, String port, String databaseName, 
                                                     String username, String password) {
        // Verificación rápida sin bloqueo
        if (instance == null) {
            LOCK.lock();
            try {
                // Verificación doble para garantizar que solo se crea una instancia
                if (instance == null) {
                    instance = new MySQLDatabaseConnection(host, port, databaseName, username, password);
                }
            } finally {
                LOCK.unlock();
            }
        }
        return instance;
    }
    
    @Override
    protected String buildJdbcUrl() {
        return String.format("jdbc:mysql://%s:%s/%s", host, port, databaseName);
    }
    
    @Override
    protected void configureConnectionProperties() {
        super.configureConnectionProperties();
        
        // Configuraciones específicas de MySQL para seguridad
        connectionProps.setProperty("requireSSL", "true");
        connectionProps.setProperty("verifyServerCertificate", "true");
        
        // Configuraciones adicionales para prevenir inyección SQL
        connectionProps.setProperty("allowMultiQueries", "false");
        
        // Configuración para proteger contra ataques JDBC URL manipulation
        connectionProps.setProperty("allowUrlInLocalInfile", "false");
        
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