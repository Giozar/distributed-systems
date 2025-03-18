package com.giozar04.servers.domain.models;

import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Representa un cliente conectado al servidor.
 * Esta clase encapsula un socket de cliente junto con su información de identificación.
 */
public class ClientConnection {
    private final Socket socket;
    private final int id;
    private final LocalDateTime connectionTime;

    /**
     * Crea una nueva instancia de ClientConnection.
     *
     * @param socket El socket de la conexión del cliente.
     * @param id El identificador único del cliente.
     * @throws NullPointerException si el socket es nulo.
     */
    public ClientConnection(Socket socket, int id) {
        this.socket = Objects.requireNonNull(socket, "El socket no puede ser nulo");
        this.id = id;
        this.connectionTime = LocalDateTime.now();
    }

    /**
     * @return El socket del cliente.
     */
    public Socket getSocket() {
        return socket;
    }

    /**
     * @return El identificador único del cliente.
     */
    public int getId() {
        return id;
    }

    /**
     * @return El momento en que se estableció la conexión.
     */
    public LocalDateTime getConnectionTime() {
        return connectionTime;
    }

    /**
     * @return La representación en cadena de texto del momento de conexión.
     */
    public String getConnectionTimeAsString() {
        return connectionTime.toString();
    }
}
