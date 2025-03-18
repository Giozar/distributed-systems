package com.giozar04.client.presentation.views;

import javax.swing.SwingUtilities;
import com.giozar04.client.application.services.ClientService;
import com.giozar04.client.presentation.views.transactions.TransactionFormFrame;
import java.io.IOException;

public class ClientMain {
    public static void main(String[] args) {
        // Inicializamos ClientService con host y puerto del servidor
        ClientService clientService = new ClientService("localhost", 8080);
        try {
            clientService.connect();
            System.out.println("Conexión establecida con el servidor.");
        } catch (IOException e) {
            System.err.println("Error al conectar con el servidor: " + e.getMessage());
            return; // Si no se conecta, no se continúa
        }
        
        // Iniciar la UI en el hilo de Swing, pasando la instancia del servicio
        SwingUtilities.invokeLater(() -> {
            TransactionFormFrame frame = new TransactionFormFrame(clientService);
            frame.setVisible(true);
        });
    }
}
