package com.giozar04.servers.domain.handlers;

import com.giozar04.servers.domain.models.ClientConnection;
import com.giozar04.servers.domain.models.Message;

/**
 * Interfaz para los manejadores de mensajes.
 * Define el contrato para procesar mensajes recibidos de los clientes.
 */
@FunctionalInterface
public interface MessageHandler {
    
    /**
     * Procesa un mensaje recibido de un cliente.
     *
     * @param clientConnection El socket del cliente que envió el mensaje.
     * @param message El mensaje recibido.
     * @return La respuesta que se enviará al cliente, o null si no hay respuesta.
     * @throws Exception Si ocurre un error durante el procesamiento.
     */
    Message handleMessage(ClientConnection clientConnection, Message message) throws Exception;
}