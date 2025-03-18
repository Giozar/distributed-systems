package com.giozar04.client.application.services;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import com.giozar04.servers.domain.models.Message;
import com.giozar04.transactions.application.utils.TransactionUtils;
import com.giozar04.transactions.domain.entities.Transaction;

/**
 * ClientService se encarga de gestionar la conexión y comunicación con el servidor vía sockets.
 * Mantiene la conexión activa, envía mensajes y escucha respuestas.
 */
public class ServerConnection {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private final String serverHost;
    private final int serverPort;
    private volatile boolean isConnected;

    public ServerConnection(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.isConnected = false;
    }

    /**
     * Establece la conexión con el servidor y lanza el hilo de escucha.
     */
    public void connect() throws IOException {
        socket = new Socket(serverHost, serverPort);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        isConnected = true;
        startListening();
    }

    /**
     * Inicia un hilo que se mantiene escuchando mensajes entrantes del servidor.
     */
    private void startListening() {
        new Thread(() -> {
            try {
                while (isConnected) {
                    Object obj = in.readObject();
                    if (obj instanceof Message) {
                        Message message = (Message) obj;
                        processIncomingMessage(message);
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                // Aquí puedes notificar a la UI o manejar la desconexión de forma controlada.
                System.err.println("Error al recibir mensajes: " + e.getMessage());
                isConnected = false;
            }
        }).start();
    }

    /**
     * Procesa el mensaje recibido del servidor.
     * Aquí podrías invocar callbacks, actualizar la UI, etc.
     */
    private void processIncomingMessage(Message message) {
        // Ejemplo básico: imprimir el mensaje recibido
        System.out.println("Mensaje recibido: " + message);
        // Aquí podrías llamar a un listener o notificar a la capa de presentación.
    }

    /**
     * Envía un mensaje al servidor.
     */
    public void sendMessage(Message message) throws IOException {
        if (socket != null && !socket.isClosed() && out != null) {
            out.writeObject(message);
            out.flush();
        } else {
            throw new IOException("No se ha establecido la conexión con el servidor");
        }
    }

    /**
     * Envuelve una transacción en un mensaje y lo envía al servidor.
     * Se convierte la transacción a Map usando TransactionUtils para cumplir con el contrato del servidor.
     */
    public void sendTransaction(Transaction transaction) throws IOException {
        Message message = new Message();
        message.setType("CREATE_TRANSACTION");
        // Convertir la Transaction a un Map antes de enviarla.
        message.addData("transaction", TransactionUtils.transactionToMap(transaction));
        sendMessage(message);
    }

    /**
     * Cierra la conexión con el servidor y libera recursos.
     */
    public void disconnect() throws IOException {
        isConnected = false;
        if (in != null) {
            in.close();
        }
        if (out != null) {
            out.close();
        }
        if (socket != null) {
            socket.close();
        }
    }
}
