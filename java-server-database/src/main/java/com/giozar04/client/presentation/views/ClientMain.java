package com.giozar04.client.presentation.views;

import java.io.IOException;

import javax.swing.SwingUtilities;

import com.giozar04.client.application.services.ServerConnection;
import com.giozar04.client.presentation.views.transactions.TransactionFormFrame;

public class ClientMain {
    public static void main(String[] args) {
        // Inicializamos ClientService con host y puerto del servidor
        ServerConnection serverConnection = new ServerConnection("localhost", 8080);
        try {
            serverConnection.connect();
            System.out.println("Conexión establecida exitosamente con el servidor.");
        } catch (IOException e) {
            System.err.println("Error al establecer la conexión con el servidor: " + e.getMessage());
            return; // Si no se conecta, no se continúa
        }
        
        // Iniciar la UI en el hilo de Swing, pasando la instancia del servicio
        SwingUtilities.invokeLater(() -> {
            TransactionFormFrame frame = new TransactionFormFrame(serverConnection);
            frame.setVisible(true);
        });
    }
}
