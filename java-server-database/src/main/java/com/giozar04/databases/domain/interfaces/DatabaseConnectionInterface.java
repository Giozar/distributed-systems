package com.giozar04.databases.domain.interfaces;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Interfaz que define las operaciones básicas para una conexión a base de datos.
 * Extiende AutoCloseable para garantizar la liberación de recursos.
 */
public interface DatabaseConnectionInterface extends AutoCloseable {
    
    /**
     * Establece la conexión con la base de datos.
     * 
     * @throws SQLException si ocurre un error durante la conexión
     */
    void connect() throws SQLException;
    
    /**
     * Cierra la conexión con la base de datos.
     * 
     * @throws SQLException si ocurre un error durante la desconexión
     */
    void disconnect() throws SQLException;
    
    /**
     * Obtiene la conexión activa a la base de datos.
     * Si no existe una conexión activa, la establece primero.
     * 
     * @return la conexión activa
     * @throws SQLException si ocurre un error al obtener la conexión
     */
    Connection getConnection() throws SQLException;
    
    /**
     * Realiza commit de la transacción actual.
     * 
     * @throws SQLException si ocurre un error al hacer commit
     */
    void commitTransaction() throws SQLException;
    
    /**
     * Realiza rollback de la transacción actual.
     * 
     * @throws SQLException si ocurre un error al hacer rollback
     */
    void rollbackTransaction() throws SQLException;
    
    /**
     * Verifica si la conexión es válida.
     * 
     * @param timeout tiempo límite en segundos para verificar la validez
     * @return true si la conexión es válida, false en caso contrario
     * @throws SQLException si ocurre un error al verificar la conexión
     */
    boolean isConnectionValid(int timeout) throws SQLException;
}