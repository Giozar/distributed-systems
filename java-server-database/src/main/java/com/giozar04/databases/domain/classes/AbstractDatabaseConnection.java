package com.giozar04.databases.domain.classes;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantLock;

import com.giozar04.databases.domain.interfaces.DatabaseConnection;
import com.giozar04.shared.CustomLogger;

/**
 * Clase abstracta que implementa funcionalidad común para conexiones a bases de datos.
 * Proporciona una implementación base para el patrón repositorio.
 */
public abstract class AbstractDatabaseConnection implements DatabaseConnection {
    
    // Lock para garantizar thread-safety durante la inicialización
    protected static final ReentrantLock LOCK = new ReentrantLock();
    
    // Conexión a la base de datos
    protected Connection connection;
    
    // Logger personalizado
    protected final CustomLogger logger;
    
    // Propiedades básicas de conexión
    protected final String host;
    protected final String port;
    protected final String databaseName;
    protected final String username;
    protected final String password;
    
    // Propiedades de conexión adicionales
    protected final Properties connectionProps;
    
    // URL JDBC
    protected final String jdbcUrl;
    
    // Tiempo máximo de espera para operaciones (en segundos)
    protected static final int DEFAULT_TIMEOUT = 30;
    
    /**
     * Constructor que inicializa los parámetros básicos de conexión.
     * 
     * @param host el host de la base de datos
     * @param port el puerto de la base de datos
     * @param databaseName el nombre de la base de datos
     * @param username el nombre de usuario para la conexión
     * @param password la contraseña para la conexión
     */
    @SuppressWarnings("OverridableMethodCallInConstructor")
    protected AbstractDatabaseConnection(String host, String port, String databaseName, 
                                      String username, String password) {
        // Validar parámetros de entrada
        this.host = Objects.requireNonNull(host, "El host no puede ser nulo");
        this.port = Objects.requireNonNull(port, "El puerto no puede ser nulo");
        this.databaseName = Objects.requireNonNull(databaseName, "El nombre de la base de datos no puede ser nulo");
        this.username = Objects.requireNonNull(username, "El nombre de usuario no puede ser nulo");
        this.password = Objects.requireNonNull(password, "La contraseña no puede ser nula");
        
        // Inicializar logger
        this.logger = new CustomLogger();
        
        // Inicializar propiedades de conexión
        this.connectionProps = new Properties();
        this.connectionProps.setProperty("user", username);
        this.connectionProps.setProperty("password", password);
        
        // Añadir propiedades básicas de seguridad
        configureConnectionProperties();
        
        // Crear URL JDBC específica para el tipo de base de datos
        this.jdbcUrl = buildJdbcUrl();
    }
    
    /**
     * Método abstracto para construir la URL JDBC específica para cada tipo de base de datos.
     * 
     * @return la URL JDBC completa
     */
    protected abstract String buildJdbcUrl();
    
    /**
     * Método para configurar propiedades de conexión adicionales.
     * Puede ser sobrescrito por las clases hijas para añadir configuraciones específicas.
     */
    protected void configureConnectionProperties() {
        // Configuraciones básicas de seguridad
        connectionProps.setProperty("useSSL", "true");
        connectionProps.setProperty("serverTimezone", "UTC");
        connectionProps.setProperty("connectTimeout", String.valueOf(DEFAULT_TIMEOUT * 1000));
    }
    
    @Override
    public boolean isConnectionValid(int timeout) throws SQLException {
        if (connection == null) {
            return false;
        }
        return connection.isValid(timeout);
    }
    
    @Override
    public void commitTransaction() throws SQLException {
        if (connection != null && !connection.isClosed() && !connection.getAutoCommit()) {
            connection.commit();
            logger.info("Transacción confirmada exitosamente");
        }
    }
    
    @Override
    public void rollbackTransaction() throws SQLException {
        if (connection != null && !connection.isClosed() && !connection.getAutoCommit()) {
            connection.rollback();
            logger.info("Rollback de transacción ejecutado");
        }
    }
    
    @Override
    public void close() throws Exception {
        disconnect();
    }
}