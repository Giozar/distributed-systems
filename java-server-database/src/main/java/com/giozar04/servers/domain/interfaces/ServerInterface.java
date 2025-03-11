package com.giozar04.servers.domain.interfaces;

import java.io.IOException;

import com.giozar04.servers.domain.exceptions.ServerOperationException;
import com.giozar04.servers.domain.models.ClientSocket;

/**
 * Define las operaciones fundamentales para un servidor de sockets.
 * Esta interfaz establece el contrato para el ciclo de vida y la gestión de clientes.
 */
public interface ServerInterface extends AutoCloseable {
    
    /**
     * Inicia el servidor y comienza a aceptar conexiones.
     * 
     * @throws ServerOperationException si ocurre un error al iniciar el servidor.
     * @throws IOException si ocurre un error de E/S durante la operación.
     */
    void startServer() throws ServerOperationException, IOException;
    
    /**
     * Detiene el servidor y cierra todas las conexiones activas.
     * 
     * @throws ServerOperationException si ocurre un error al detener el servidor.
     */
    void stopServer() throws ServerOperationException;
    
    /**
     * Reinicia el servidor (detiene y vuelve a iniciar).
     * 
     * @throws ServerOperationException si ocurre un error durante el reinicio.
     * @throws IOException si ocurre un error de E/S durante la operación.
     */
    void restartServer() throws ServerOperationException, IOException;
    
    /**
     * Verifica si el servidor está en ejecución.
     * 
     * @return true si el servidor está ejecutándose, false en caso contrario.
     * @throws ServerOperationException si ocurre un error al verificar el estado.
     */
    boolean isServerRunning() throws ServerOperationException;
    
    /**
     * Maneja una conexión de cliente específica.
     * 
     * @param clientSocket El cliente a manejar.
     * @throws ServerOperationException si ocurre un error al manejar el cliente.
     */
    void handleClientSocket(ClientSocket clientSocket) throws ServerOperationException;
    
    /**
     * Acepta nuevas conexiones de clientes (normalmente ejecutado en un bucle).
     * 
     * @throws ServerOperationException si ocurre un error al aceptar conexiones.
     * @throws IOException si ocurre un error de E/S durante la operación.
     */
    void acceptClientSocketConnections() throws ServerOperationException, IOException;
    
    /**
     * Obtiene el número de clientes actualmente conectados.
     * 
     * @return El número de clientes conectados.
     * @throws ServerOperationException si ocurre un error al obtener el conteo.
     */
    int getConnectedClientSocketsCount() throws ServerOperationException;
}