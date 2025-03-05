package concurrence.server.domain;

import java.net.Socket;

public class Client {

    private final Socket socket;
    private final int id;
    private final String connectionTime;

    public Client(Socket socket, int id) {
        this.socket = socket;
        this.id = id;
        this.connectionTime = java.time.LocalDateTime.now().toString();
    }


    public Socket getSocket() {
        return socket;
    }
    public int getId() {
        return id;
    }
    public String getConnectionTime() {
        return connectionTime;
    }
}
