package com.giozar04.client.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import com.giozar04.servers.domain.models.Message;

/**
 * Monitor de clientes conectados al servidor de sockets
 */
public class SocketClientMonitor extends JFrame {

    // Configuración del servidor
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8080;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // Componentes de la interfaz
    private JLabel statusLabel;
    private JLabel connectedClientsLabel;
    private JButton connectButton;
    private JButton disconnectButton;
    private JTable clientsTable;
    private DefaultTableModel tableModel;
    private JTextArea logArea;
    
    // Botones para simular carga
    private JButton addClientButton;
    private JSpinner clientCountSpinner;
    
    // Socket y streams
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private boolean connected = false;
    private int myClientId = -1;
    
    // Lista para mantener registro de los clientes simulados
    private List<SimulatedClient> simulatedClients = new ArrayList<>();
    private AtomicInteger nextClientId = new AtomicInteger(1);
    
    /**
     * Constructor principal
     */
    public SocketClientMonitor() {
        // Configuración básica de la ventana
        setTitle("Monitor de Clientes Socket");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // Inicializar componentes
        initComponents();
        
        // Organizar el layout
        setupLayout();
        
        // Configurar listeners
        setupListeners();
        
        // Configuración de cierre de ventana
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                disconnectAllClients();
                disconnect();
            }
        });
    }
    
    /**
     * Inicializa los componentes de la interfaz
     */
    private void initComponents() {
        // Panel de estado
        statusLabel = new JLabel("Desconectado");
        statusLabel.setForeground(Color.RED);
        connectedClientsLabel = new JLabel("Clientes conectados: 0");
        
        // Botones de conexión
        connectButton = new JButton("Conectar Monitor");
        disconnectButton = new JButton("Desconectar Monitor");
        disconnectButton.setEnabled(false);
        
        // Controles para simular carga
        clientCountSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        addClientButton = new JButton("Agregar Clientes");
        addClientButton.setEnabled(false);
        
        // Tabla de clientes
        String[] columns = {"ID", "Dirección IP", "Puerto", "Hora de Conexión", "Estado"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;  // Hacer que la tabla no sea editable
            }
        };
        clientsTable = new JTable(tableModel);
        clientsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Área de logs
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    }
    
    /**
     * Configura el layout de la interfaz
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Panel superior (conexión y estado)
        JPanel topPanel = new JPanel(new BorderLayout());
        
        JPanel statusPanel = new JPanel();
        statusPanel.add(statusLabel);
        statusPanel.add(connectedClientsLabel);
        
        JPanel connectionPanel = new JPanel();
        connectionPanel.add(connectButton);
        connectionPanel.add(disconnectButton);
        
        topPanel.add(statusPanel, BorderLayout.WEST);
        topPanel.add(connectionPanel, BorderLayout.EAST);
        
        add(topPanel, BorderLayout.NORTH);
        
        // Panel central (tabla y simulación)
        JPanel centralPanel = new JPanel(new BorderLayout());
        
        // Panel de simulación
        JPanel simulationPanel = new JPanel();
        simulationPanel.setBorder(BorderFactory.createTitledBorder("Simulación de Carga"));
        simulationPanel.add(new JLabel("Número de clientes:"));
        simulationPanel.add(clientCountSpinner);
        simulationPanel.add(addClientButton);
        
        centralPanel.add(simulationPanel, BorderLayout.NORTH);
        
        // Tabla de clientes en un panel con scroll
        JScrollPane tableScrollPane = new JScrollPane(clientsTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder("Clientes Conectados"));
        centralPanel.add(tableScrollPane, BorderLayout.CENTER);
        
        add(centralPanel, BorderLayout.CENTER);
        
        // Panel inferior (logs)
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Registro de Eventos"));
        logScrollPane.setPreferredSize(new Dimension(800, 200));
        
        add(logScrollPane, BorderLayout.SOUTH);
    }
    
    /**
     * Configura los event listeners
     */
    private void setupListeners() {
        // Listeners de conexión
        connectButton.addActionListener(e -> connect());
        disconnectButton.addActionListener(e -> {
            disconnectAllClients();
            disconnect();
        });
        
        // Listener para agregar clientes
        addClientButton.addActionListener(e -> addSimulatedClients());
    }
    
    /**
     * Agrega un mensaje al log
     */
    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
            logArea.append("[" + timestamp + "] " + message + "\n");
            // Scroll al final
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    /**
     * Conecta con el servidor
     */
    private void connect() {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            
            connected = true;
            
            // Actualizar UI
            statusLabel.setText("Conectado");
            statusLabel.setForeground(Color.GREEN);
            connectButton.setEnabled(false);
            disconnectButton.setEnabled(true);
            addClientButton.setEnabled(true);
            
            // Iniciar hilo para recibir mensajes
            new Thread(this::receiveMessages).start();
            
            log("Monitor conectado al servidor en " + SERVER_HOST + ":" + SERVER_PORT);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error al conectar: " + e.getMessage(), 
                "Error de Conexión", 
                JOptionPane.ERROR_MESSAGE);
            log("Error al conectar: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Desconecta del servidor
     */
    private void disconnect() {
        if (connected) {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
                connected = false;
                
                // Actualizar UI
                statusLabel.setText("Desconectado");
                statusLabel.setForeground(Color.RED);
                connectButton.setEnabled(true);
                disconnectButton.setEnabled(false);
                addClientButton.setEnabled(false);
                
                log("Monitor desconectado del servidor");
                
            } catch (Exception e) {
                log("Error al desconectar: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Hilo para recibir mensajes del servidor
     */
    private void receiveMessages() {
        try {
            while (connected) {
                Object receivedObject = in.readObject();
                
                if (receivedObject instanceof Message) {
                    Message message = (Message) receivedObject;
                    
                    // Procesar el mensaje según su tipo
                    if (message.getType().equals("WELCOME")) {
                        handleWelcomeMessage(message);
                    } else {
                        log("Mensaje recibido: " + message.getType() + " - " + message.getContent());
                    }
                }
            }
        } catch (IOException e) {
            if (connected) {
                log("Error de conexión: " + e.getMessage());
                SwingUtilities.invokeLater(this::disconnect);
            }
        } catch (ClassNotFoundException e) {
            log("Error al deserializar objeto: " + e.getMessage());
        } catch (Exception e) {
            log("Error inesperado: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Procesa el mensaje de bienvenida
     */
    private void handleWelcomeMessage(Message message) {
        String content = message.getContent();
        log("Mensaje de bienvenida: " + content);
        
        // Extraer el ID del cliente
        if (content != null && content.contains("Cliente ID:")) {
            String[] parts = content.split("Cliente ID: ");
            if (parts.length > 1) {
                try {
                    myClientId = Integer.parseInt(parts[1].trim());
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Conectado (ID: " + myClientId + ")");
                        addMonitorToTable();
                    });
                } catch (NumberFormatException e) {
                    log("Error al parsear ID de cliente: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Agrega el monitor a la tabla de clientes
     */
    private void addMonitorToTable() {
        String ip = socket.getLocalAddress().getHostAddress();
        int port = socket.getLocalPort();
        String time = LocalDateTime.now().format(TIME_FORMATTER);
        
        Object[] rowData = {
            myClientId,
            ip,
            port,
            time,
            "Monitor Activo"
        };
        
        tableModel.addRow(rowData);
        updateClientCount();
    }
    
    /**
     * Agrega clientes simulados
     */
    private void addSimulatedClients() {
        int count = (Integer) clientCountSpinner.getValue();
        
        for (int i = 0; i < count; i++) {
            addSimulatedClient();
        }
    }
    
    /**
     * Agrega un cliente simulado
     */
    private void addSimulatedClient() {
        try {
            SimulatedClient client = new SimulatedClient();
            simulatedClients.add(client);
            client.connect();
            
        } catch (Exception e) {
            log("Error al crear cliente simulado: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Desconecta todos los clientes simulados
     */
    private void disconnectAllClients() {
        for (SimulatedClient client : simulatedClients) {
            try {
                client.disconnect();
            } catch (Exception e) {
                log("Error al desconectar cliente " + client.id + ": " + e.getMessage());
            }
        }
        simulatedClients.clear();
    }
    
    /**
     * Actualiza el contador de clientes conectados
     */
    private void updateClientCount() {
        int count = tableModel.getRowCount();
        connectedClientsLabel.setText("Clientes conectados: " + count);
    }
    
    /**
     * Clase interna para gestionar clientes simulados
     */
    private class SimulatedClient {
        private Socket socket;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        private int id;
        private Thread receiverThread;
        private boolean connected = false;
        
        /**
         * Conecta al servidor
         */
        public void connect() throws IOException {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;
            
            // Iniciar hilo para recibir mensajes
            receiverThread = new Thread(this::receiveMessages);
            receiverThread.start();
            
            log("Cliente simulado conectado al servidor");
        }
        
        /**
         * Desconecta del servidor
         */
        public void disconnect() {
            if (connected) {
                try {
                    if (socket != null && !socket.isClosed()) {
                        socket.close();
                    }
                    connected = false;
                    
                    // Remover de la tabla
                    removeClientFromTable(id);
                    
                    log("Cliente simulado " + id + " desconectado");
                    
                } catch (Exception e) {
                    log("Error al desconectar cliente simulado " + id + ": " + e.getMessage());
                }
            }
        }
        
        /**
         * Recibe mensajes del servidor
         */
        private void receiveMessages() {
            try {
                while (connected) {
                    Object receivedObject = in.readObject();
                    
                    if (receivedObject instanceof Message) {
                        Message message = (Message) receivedObject;
                        
                        if (message.getType().equals("WELCOME")) {
                            String content = message.getContent();
                            
                            // Extraer el ID del cliente
                            if (content != null && content.contains("Cliente ID:")) {
                                String[] parts = content.split("Cliente ID: ");
                                if (parts.length > 1) {
                                    id = Integer.parseInt(parts[1].trim());
                                    addClientToTable(id);
                                    log("Cliente simulado recibió ID: " + id);
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    log("Cliente simulado " + id + " perdió conexión: " + e.getMessage());
                    removeClientFromTable(id);
                }
            } catch (ClassNotFoundException e) {
                log("Error al deserializar objeto en cliente " + id + ": " + e.getMessage());
            } catch (Exception e) {
                log("Error inesperado en cliente " + id + ": " + e.getMessage());
            }
        }
        
        /**
         * Agrega el cliente a la tabla
         */
        private void addClientToTable(int clientId) {
            SwingUtilities.invokeLater(() -> {
                String ip = socket.getLocalAddress().getHostAddress();
                int port = socket.getLocalPort();
                String time = LocalDateTime.now().format(TIME_FORMATTER);
                
                Object[] rowData = {
                    clientId,
                    ip,
                    port,
                    time,
                    "Cliente Simulado"
                };
                
                tableModel.addRow(rowData);
                updateClientCount();
            });
        }
        
        /**
         * Remueve el cliente de la tabla
         */
        private void removeClientFromTable(int clientId) {
            SwingUtilities.invokeLater(() -> {
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    Object idObj = tableModel.getValueAt(i, 0);
                    if (idObj != null && Integer.parseInt(idObj.toString()) == clientId) {
                        tableModel.removeRow(i);
                        break;
                    }
                }
                updateClientCount();
            });
        }
    }
    
    /**
     * Punto de entrada principal
     */
    public static void main(String[] args) {
        try {
            // Intentar usar el look and feel del sistema
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> {
            SocketClientMonitor monitor = new SocketClientMonitor();
            monitor.setVisible(true);
        });
    }
}