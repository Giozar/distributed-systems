package com.giozar04.databases.domain.exceptions;

import java.sql.SQLException;

/**
 * Clase contenedora para las excepciones relacionadas con operaciones de base de datos.
 * Proporciona un conjunto de excepciones específicas para diferentes tipos de errores.
 */
public class DatabaseExceptions {

    /**
     * Excepción base para todos los errores relacionados con la base de datos.
     * Extiende RuntimeException para evitar la declaración obligatoria en la firma de los métodos.
     */
    public static class DatabaseException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public DatabaseException(String message) {
            super(message);
        }

        public DatabaseException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Excepción lanzada cuando hay un problema al establecer la conexión con la base de datos.
     */
    public static class ConnectionException extends DatabaseException {
        private static final long serialVersionUID = 1L;

        public ConnectionException(String message) {
            super(message);
        }

        public ConnectionException(String message, Throwable cause) {
            super(message, cause);
        }
        
        /**
         * Método de utilidad para convertir SQLException en ConnectionException.
         */
        public static ConnectionException fromSQLException(SQLException e) {
            return new ConnectionException("Error de conexión a la base de datos: " + e.getMessage(), e);
        }
    }

    /**
     * Excepción lanzada cuando falla una operación de consulta (SELECT).
     */
    public static class QueryException extends DatabaseException {
        private static final long serialVersionUID = 1L;

        public QueryException(String message) {
            super(message);
        }

        public QueryException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Excepción lanzada cuando falla una operación de actualización (INSERT, UPDATE, DELETE).
     */
    public static class UpdateException extends DatabaseException {
        private static final long serialVersionUID = 1L;

        public UpdateException(String message) {
            super(message);
        }

        public UpdateException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Excepción lanzada cuando falla una transacción.
     */
    public static class TransactionException extends DatabaseException {
        private static final long serialVersionUID = 1L;

        public TransactionException(String message) {
            super(message);
        }

        public TransactionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Excepción lanzada cuando se intenta realizar una operación con una conexión cerrada.
     */
    public static class ConnectionClosedException extends DatabaseException {
        private static final long serialVersionUID = 1L;

        public ConnectionClosedException(String message) {
            super(message);
        }

        public ConnectionClosedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Excepción lanzada cuando hay un problema con la configuración de la base de datos.
     */
    public static class ConfigurationException extends DatabaseException {
        private static final long serialVersionUID = 1L;

        public ConfigurationException(String message) {
            super(message);
        }

        public ConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Excepción lanzada cuando se detecta un problema de pool de conexiones.
     */
    public static class ConnectionPoolException extends DatabaseException {
        private static final long serialVersionUID = 1L;

        public ConnectionPoolException(String message) {
            super(message);
        }

        public ConnectionPoolException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Excepción lanzada cuando hay un error específico del driver de la base de datos.
     */
    public static class DriverException extends DatabaseException {
        private static final long serialVersionUID = 1L;

        public DriverException(String message) {
            super(message);
        }

        public DriverException(String message, Throwable cause) {
            super(message, cause);
        }
        
        /**
         * Método de utilidad para convertir ClassNotFoundException en DriverException.
         */
        public static DriverException fromClassNotFoundException(ClassNotFoundException e) {
            return new DriverException("Driver de base de datos no encontrado: " + e.getMessage(), e);
        }
    }

    /**
     * Excepción lanzada cuando se detecta un error de acceso o permisos.
     */
    public static class AccessDeniedException extends DatabaseException {
        private static final long serialVersionUID = 1L;

        public AccessDeniedException(String message) {
            super(message);
        }

        public AccessDeniedException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Excepción lanzada cuando se detecta un intento de inyección SQL o un problema de seguridad.
     */
    public static class SecurityException extends DatabaseException {
        private static final long serialVersionUID = 1L;

        public SecurityException(String message) {
            super(message);
        }

        public SecurityException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Utilidad para analizar SQLException y convertirla en la excepción más apropiada.
     * 
     * @param e La excepción SQL original
     * @param operationType Descripción de la operación que falló
     * @return Una subclase apropiada de DatabaseException
     */
    public static DatabaseException translateSQLException(SQLException e, String operationType) {
        String sqlState = e.getSQLState();
        int errorCode = e.getErrorCode();
        
        // Códigos comunes para conexiones fallidas
        if (sqlState != null && (sqlState.startsWith("08") || sqlState.equals("HY000"))) {
            return new ConnectionException("Error de conexión durante " + operationType + ": " + e.getMessage(), e);
        }
        
        // Códigos comunes para acceso denegado
        if (sqlState != null && sqlState.startsWith("28")) {
            return new AccessDeniedException("Acceso denegado durante " + operationType + ": " + e.getMessage(), e);
        }
        
        // Códigos comunes para errores de sintaxis SQL (posible inyección)
        if (sqlState != null && sqlState.startsWith("42")) {
            return new SecurityException("Posible problema de seguridad en " + operationType + ": " + e.getMessage(), e);
        }
        
        // MySQL: Error de duplicado
        if (errorCode == 1062) {
            return new UpdateException("Entrada duplicada en " + operationType + ": " + e.getMessage(), e);
        }
        
        // Si es un SELECT, usar QueryException
        if (operationType.toLowerCase().contains("select") || 
            operationType.toLowerCase().contains("consulta")) {
            return new QueryException("Error en consulta " + operationType + ": " + e.getMessage(), e);
        }
        
        // Si es una operación de modificación, usar UpdateException
        if (operationType.toLowerCase().contains("insert") || 
            operationType.toLowerCase().contains("update") || 
            operationType.toLowerCase().contains("delete")) {
            return new UpdateException("Error en actualización " + operationType + ": " + e.getMessage(), e);
        }
        
        // Por defecto
        return new DatabaseException("Error en operación de base de datos " + operationType + ": " + e.getMessage(), e);
    }
}