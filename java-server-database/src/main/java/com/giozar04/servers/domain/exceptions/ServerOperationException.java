package com.giozar04.servers.domain.exceptions;

/**
 * Excepción específica para operaciones relacionadas con el servidor de sockets.
 * Proporciona información detallada sobre errores durante las operaciones del servidor.
 */
public class ServerOperationException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Construye una nueva excepción con el mensaje especificado.
     *
     * @param message El mensaje de detalle.
     */
    public ServerOperationException(String message) {
        super(message);
    }

    /**
     * Construye una nueva excepción con el mensaje y la causa especificados.
     *
     * @param message El mensaje de detalle.
     * @param cause La causa de la excepción.
     */
    public ServerOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Construye una nueva excepción con la causa especificada.
     *
     * @param cause La causa de la excepción.
     */
    public ServerOperationException(Throwable cause) {
        super(cause);
    }
}