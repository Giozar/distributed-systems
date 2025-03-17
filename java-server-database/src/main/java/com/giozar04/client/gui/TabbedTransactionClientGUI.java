package com.giozar04.client.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import com.giozar04.databases.domain.interfaces.DatabaseConnectionInterface;
import com.giozar04.databases.infrastructure.repositories.DatabaseConnectionMySQL;
import com.giozar04.transactions.application.services.TransactionService;
import com.giozar04.transactions.domain.entities.Transaction;
import com.giozar04.transactions.domain.enums.PaymentMethod;
import com.giozar04.transactions.infrastructure.repositories.TransactionRepositoryMySQL;

/**
 * Cliente GUI para gestionar transacciones con pestañas separadas para cada operación CRUD
 */
public class TabbedTransactionClientGUI extends JFrame {

    // Configuración de la base de datos
    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "finanzas";
    private static final String DB_USER = "giovanni";
    private static final String DB_PASSWORD = "finanzas123";
    
    // Componentes principales
    private JTabbedPane tabbedPane;
    private JLabel statusLabel;
    
    // Servicios y conexiones
    private DatabaseConnectionInterface dbConnection;
    private TransactionService transactionService;
    private boolean connected = false;
    
    // Componentes de la pestaña Listar
    private JTable listTransactionsTable;
    private DefaultTableModel listTableModel;
    private JButton listRefreshButton;
    private JButton listEditButton;
    private JButton listDeleteButton;
    
    // Componentes de la pestaña Crear
    private JTextField createTitleField;
    private JTextField createAmountField;
    private JComboBox<String> createTypeComboBox;
    private JComboBox<String> createPaymentMethodComboBox;
    private JTextField createCategoryField;
    private JTextArea createDescriptionArea;
    private JTextField createTagsField;
    private JButton createButton;
    private JButton createClearButton;
    
    // Componentes de la pestaña Editar
    private JLabel editIdLabel;
    private JTextField editTitleField;
    private JTextField editAmountField;
    private JComboBox<String> editTypeComboBox;
    private JComboBox<String> editPaymentMethodComboBox;
    private JTextField editCategoryField;
    private JTextArea editDescriptionArea;
    private JTextField editTagsField;
    private JButton updateButton;
    private JButton cancelEditButton;
    
    // Datos de la transacción actual para editar
    private Transaction currentTransaction;
    
    /**
     * Constructor principal
     */
    public TabbedTransactionClientGUI() {
        // Configuración básica de la ventana
        setTitle("Gestor de Transacciones");
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
                disconnectFromDB();
            }
        });
        
        // Conectar a la base de datos al iniciar
        connectToDB();
    }
    
    /**
     * Inicializa los componentes de la interfaz
     */
    private void initComponents() {
        // Panel principal con pestañas
        tabbedPane = new JTabbedPane();
        
        // Panel de estado
        statusLabel = new JLabel("Desconectado");
        statusLabel.setForeground(Color.RED);
        
        // Inicializar componentes de cada pestaña
        initListComponents();
        initCreateComponents();
        initEditComponents();
    }
    
    /**
     * Inicializa los componentes de la pestaña Listar
     */
    private void initListComponents() {
        // Tabla de transacciones
        String[] columns = {"ID", "Título", "Monto", "Tipo", "Método de Pago", "Categoría", "Fecha"};
        listTableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;  // Hacer que la tabla no sea editable
            }
        };
        listTransactionsTable = new JTable(listTableModel);
        listTransactionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Botones
        listRefreshButton = new JButton("Refrescar");
        listEditButton = new JButton("Editar");
        listDeleteButton = new JButton("Eliminar");
        
        // Estado inicial
        listEditButton.setEnabled(false);
        listDeleteButton.setEnabled(false);
    }
    
    /**
     * Inicializa los componentes de la pestaña Crear
     */
    private void initCreateComponents() {
        // Campos de formulario
        createTitleField = new JTextField(20);
        createAmountField = new JTextField(10);
        createTypeComboBox = new JComboBox<>(new String[]{"EXPENSE", "INCOME"});
        createPaymentMethodComboBox = new JComboBox<>(new String[]{
            "CASH", "CARD_DEBIT", "CARD_CREDIT"
        });
        createCategoryField = new JTextField(15);
        createDescriptionArea = new JTextArea(5, 20);
        createDescriptionArea.setLineWrap(true);
        createTagsField = new JTextField(20);
        
        // Botones
        createButton = new JButton("Crear Transacción");
        createClearButton = new JButton("Limpiar Campos");
    }
    
    /**
     * Inicializa los componentes de la pestaña Editar
     */
    private void initEditComponents() {
        // Campos de formulario
        editIdLabel = new JLabel("ID: ");
        editTitleField = new JTextField(20);
        editAmountField = new JTextField(10);
        editTypeComboBox = new JComboBox<>(new String[]{"EXPENSE", "INCOME"});
        editPaymentMethodComboBox = new JComboBox<>(new String[]{
            "CASH", "CARD_DEBIT", "CARD_CREDIT"
        });
        editCategoryField = new JTextField(15);
        editDescriptionArea = new JTextArea(5, 20);
        editDescriptionArea.setLineWrap(true);
        editTagsField = new JTextField(20);
        
        // Botones
        updateButton = new JButton("Actualizar Transacción");
        cancelEditButton = new JButton("Cancelar");
    }
    
    /**
     * Configura el layout de la interfaz
     */
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        // Panel de estado (superior)
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusPanel.add(statusLabel);
        add(statusPanel, BorderLayout.NORTH);
        
        // Configurar paneles para cada pestaña
        JPanel listPanel = setupListPanel();
        JPanel createPanel = setupCreatePanel();
        JPanel editPanel = setupEditPanel();
        
        // Añadir pestañas
        tabbedPane.addTab("Listar Transacciones", null, listPanel, "Ver todas las transacciones");
        tabbedPane.addTab("Crear Transacción", null, createPanel, "Crear una nueva transacción");
        tabbedPane.addTab("Editar Transacción", null, editPanel, "Editar transacción existente");
        
        // Deshabilitar pestaña de edición inicialmente
        tabbedPane.setEnabledAt(2, false);
        
        // Añadir panel de pestañas al centro
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    /**
     * Configura el panel de la pestaña Listar
     */
    private JPanel setupListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        // Panel superior con botones
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.add(listRefreshButton);
        panel.add(topPanel, BorderLayout.NORTH);
        
        // Tabla con scroll en el centro
        JScrollPane scrollPane = new JScrollPane(listTransactionsTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        // Panel inferior con botones de acción
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.add(listEditButton);
        bottomPanel.add(listDeleteButton);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    /**
     * Configura el panel de la pestaña Crear
     */
    private JPanel setupCreatePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Título de la sección
        JLabel titleLabel = new JLabel("Crear Nueva Transacción");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        formPanel.add(titleLabel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // Panel de campos del formulario
        JPanel fieldsPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        
        fieldsPanel.add(new JLabel("Título:"));
        fieldsPanel.add(createTitleField);
        
        fieldsPanel.add(new JLabel("Monto:"));
        fieldsPanel.add(createAmountField);
        
        fieldsPanel.add(new JLabel("Tipo:"));
        fieldsPanel.add(createTypeComboBox);
        
        fieldsPanel.add(new JLabel("Método de Pago:"));
        fieldsPanel.add(createPaymentMethodComboBox);
        
        fieldsPanel.add(new JLabel("Categoría:"));
        fieldsPanel.add(createCategoryField);
        
        fieldsPanel.add(new JLabel("Etiquetas (separadas por coma):"));
        fieldsPanel.add(createTagsField);
        
        fieldsPanel.add(new JLabel("Descripción:"));
        JScrollPane descScrollPane = new JScrollPane(createDescriptionArea);
        descScrollPane.setPreferredSize(new Dimension(200, 100));
        fieldsPanel.add(descScrollPane);
        
        formPanel.add(fieldsPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // Panel de botones
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonsPanel.add(createButton);
        buttonsPanel.add(createClearButton);
        formPanel.add(buttonsPanel);
        
        // Añadir todo al panel principal
        panel.add(new JScrollPane(formPanel), BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Configura el panel de la pestaña Editar
     */
    private JPanel setupEditPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Título de la sección
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel titleLabel = new JLabel("Editar Transacción - ");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        headerPanel.add(titleLabel);
        headerPanel.add(editIdLabel);
        formPanel.add(headerPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // Panel de campos del formulario
        JPanel fieldsPanel = new JPanel(new GridLayout(0, 2, 10, 10));
        
        fieldsPanel.add(new JLabel("Título:"));
        fieldsPanel.add(editTitleField);
        
        fieldsPanel.add(new JLabel("Monto:"));
        fieldsPanel.add(editAmountField);
        
        fieldsPanel.add(new JLabel("Tipo:"));
        fieldsPanel.add(editTypeComboBox);
        
        fieldsPanel.add(new JLabel("Método de Pago:"));
        fieldsPanel.add(editPaymentMethodComboBox);
        
        fieldsPanel.add(new JLabel("Categoría:"));
        fieldsPanel.add(editCategoryField);
        
        fieldsPanel.add(new JLabel("Etiquetas (separadas por coma):"));
        fieldsPanel.add(editTagsField);
        
        fieldsPanel.add(new JLabel("Descripción:"));
        JScrollPane descScrollPane = new JScrollPane(editDescriptionArea);
        descScrollPane.setPreferredSize(new Dimension(200, 100));
        fieldsPanel.add(descScrollPane);
        
        formPanel.add(fieldsPanel);
        formPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        
        // Panel de botones
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonsPanel.add(updateButton);
        buttonsPanel.add(cancelEditButton);
        formPanel.add(buttonsPanel);
        
        // Añadir todo al panel principal
        panel.add(new JScrollPane(formPanel), BorderLayout.CENTER);
        
        return panel;
    }
    
    /**
     * Configura los event listeners
     */
    private void setupListeners() {
        // Listeners de la tabla de listado
        listTransactionsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && listTransactionsTable.getSelectedRow() != -1) {
                listEditButton.setEnabled(true);
                listDeleteButton.setEnabled(true);
            } else {
                listEditButton.setEnabled(false);
                listDeleteButton.setEnabled(false);
            }
        });
        
        // Listeners de botones de la pestaña Listar
        listRefreshButton.addActionListener(e -> refreshTransactions());
        listEditButton.addActionListener(e -> openEditTab());
        listDeleteButton.addActionListener(e -> deleteSelectedTransaction());
        
        // Listeners de botones de la pestaña Crear
        createButton.addActionListener(e -> createTransaction());
        createClearButton.addActionListener(e -> clearCreateForm());
        
        // Listeners de botones de la pestaña Editar
        updateButton.addActionListener(e -> updateTransaction());
        cancelEditButton.addActionListener(e -> cancelEdit());
    }
    
    /**
     * Conecta con la base de datos
     */
    private void connectToDB() {
        try {
            // Inicializar conexión a la base de datos
            dbConnection = DatabaseConnectionMySQL.getInstance(
                DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD);
            
            // Conectar a la base de datos
            dbConnection.connect();
            
            // Crear el repositorio y servicio
            TransactionRepositoryMySQL transactionRepo = new TransactionRepositoryMySQL(dbConnection);
            transactionService = new TransactionService(transactionRepo);
            
            connected = true;
            
            // Actualizar UI
            statusLabel.setText("Conectado a la base de datos");
            statusLabel.setForeground(Color.GREEN);
            
            // Cargar transacciones iniciales
            refreshTransactions();
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error al conectar a la base de datos: " + e.getMessage(), 
                "Error de Conexión", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            
            statusLabel.setText("Error de conexión");
            statusLabel.setForeground(Color.RED);
        }
    }
    
    /**
     * Desconecta de la base de datos
     */
    private void disconnectFromDB() {
        if (connected && dbConnection != null) {
            try {
                dbConnection.disconnect();
                connected = false;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Refresca la lista de transacciones
     */
    private void refreshTransactions() {
        if (!connected) {
            return;
        }
        
        try {
            // Obtener todas las transacciones
            List<Transaction> transactions = transactionService.getAllTransactions();
            
            // Actualizar tabla
            updateTransactionsTable(transactions);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error al obtener transacciones: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    /**
     * Actualiza la tabla con las transacciones
     */
    private void updateTransactionsTable(List<Transaction> transactions) {
        // Limpiar tabla
        listTableModel.setRowCount(0);
        
        // Agregar filas para cada transacción
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (Transaction transaction : transactions) {
            Object[] rowData = {
                transaction.getId(),
                transaction.getTitle(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getPaymentMethod().name(),
                transaction.getCategory(),
                transaction.getDate().format(formatter)
            };
            listTableModel.addRow(rowData);
        }
    }
    
    /**
     * Abre la pestaña de edición con la transacción seleccionada
     */
    private void openEditTab() {
        int selectedRow = listTransactionsTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        
        try {
            long id = (long) listTableModel.getValueAt(selectedRow, 0);
            
            // Cargar la transacción completa
            Transaction transaction = transactionService.getTransactionById(id);
            if (transaction == null) {
                JOptionPane.showMessageDialog(this, 
                    "No se pudo cargar la transacción seleccionada", 
                    "Error", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Guardar referencia a la transacción actual
            currentTransaction = transaction;
            
            // Cargar datos en el formulario de edición
            editIdLabel.setText("ID: " + transaction.getId());
            editTitleField.setText(transaction.getTitle());
            editAmountField.setText(String.valueOf(transaction.getAmount()));
            editTypeComboBox.setSelectedItem(transaction.getType());
            editPaymentMethodComboBox.setSelectedItem(transaction.getPaymentMethod().name());
            editCategoryField.setText(transaction.getCategory());
            editDescriptionArea.setText(transaction.getDescription());
            
            // Cargar etiquetas
            if (transaction.getTags() != null && !transaction.getTags().isEmpty()) {
                editTagsField.setText(String.join(", ", transaction.getTags()));
            } else {
                editTagsField.setText("");
            }
            
            // Activar pestaña de edición y cambiar a ella
            tabbedPane.setEnabledAt(2, true);
            tabbedPane.setSelectedIndex(2);
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error al cargar transacción para edición: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    /**
     * Cancela la edición y vuelve a la pestaña de listado
     */
    private void cancelEdit() {
        // Limpiar datos
        currentTransaction = null;
        
        // Deshabilitar pestaña de edición y volver a listado
        tabbedPane.setEnabledAt(2, false);
        tabbedPane.setSelectedIndex(0);
    }
    
    /**
     * Elimina la transacción seleccionada
     */
    private void deleteSelectedTransaction() {
        int selectedRow = listTransactionsTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }
        
        try {
            long id = (long) listTableModel.getValueAt(selectedRow, 0);
            
            // Confirmar eliminación
            int confirm = JOptionPane.showConfirmDialog(this, 
                "¿Está seguro de eliminar esta transacción?", 
                "Confirmar Eliminación", 
                JOptionPane.YES_NO_OPTION);
                
            if (confirm == JOptionPane.YES_OPTION) {
                // Eliminar la transacción
                transactionService.deleteTransactionById(id);
                
                // Actualizar la tabla
                refreshTransactions();
                
                JOptionPane.showMessageDialog(this, 
                    "Transacción eliminada correctamente", 
                    "Éxito", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
            
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error al eliminar transacción: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    /**
     * Crea una nueva transacción
     */
    private void createTransaction() {
        try {
            // Validar campos requeridos
            if (createTitleField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "El título es obligatorio", 
                    "Error de validación", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (createAmountField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "El monto es obligatorio", 
                    "Error de validación", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Crear una nueva transacción
            Transaction transaction = new Transaction();
            fillTransactionFromCreateForm(transaction);
            
            // Guardar en la base de datos
            Transaction savedTransaction = transactionService.createTransaction(transaction);
            
            // Limpiar formulario
            clearCreateForm();
            
            // Actualizar tabla y cambiar a pestaña de listado
            refreshTransactions();
            tabbedPane.setSelectedIndex(0);
            
            JOptionPane.showMessageDialog(this, 
                "Transacción creada correctamente con ID: " + savedTransaction.getId(), 
                "Éxito", 
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, 
                "El monto debe ser un número válido", 
                "Error de formato", 
                JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error al crear transacción: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    /**
     * Actualiza la transacción actual
     */
    private void updateTransaction() {
        if (currentTransaction == null) {
            return;
        }
        
        try {
            // Validar campos requeridos
            if (editTitleField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "El título es obligatorio", 
                    "Error de validación", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            if (editAmountField.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, 
                    "El monto es obligatorio", 
                    "Error de validación", 
                    JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            // Actualizar datos de la transacción
            fillTransactionFromEditForm(currentTransaction);
            
            // Guardar cambios
            transactionService.updateTransactionById(currentTransaction.getId(), currentTransaction);
            
            // Actualizar tabla y volver a pestaña de listado
            refreshTransactions();
            cancelEdit();
            
            JOptionPane.showMessageDialog(this, 
                "Transacción actualizada correctamente", 
                "Éxito", 
                JOptionPane.INFORMATION_MESSAGE);
                
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, 
                "El monto debe ser un número válido", 
                "Error de formato", 
                JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, 
                "Error al actualizar transacción: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    
    /**
     * Llena la transacción con datos del formulario de creación
     */
    private void fillTransactionFromCreateForm(Transaction transaction) {
        transaction.setTitle(createTitleField.getText().trim());
        transaction.setAmount(Double.parseDouble(createAmountField.getText().trim()));
        transaction.setType((String) createTypeComboBox.getSelectedItem());
        transaction.setPaymentMethod(PaymentMethod.valueOf(
            (String) createPaymentMethodComboBox.getSelectedItem()));
        transaction.setCategory(createCategoryField.getText().trim());
        transaction.setDescription(createDescriptionArea.getText().trim());
        
        // Procesar etiquetas
        String tagsText = createTagsField.getText().trim();
        if (!tagsText.isEmpty()) {
            List<String> tags = Arrays.asList(tagsText.split("\\s*,\\s*"));
            transaction.setTags(tags);
        } else {
            transaction.setTags(new ArrayList<>());
        }
        
        // Establecer fecha actual
        transaction.setDate(ZonedDateTime.now(ZoneId.systemDefault()));
    }
    
    /**
     * Llena la transacción con datos del formulario de edición
     */
    private void fillTransactionFromEditForm(Transaction transaction) {
        transaction.setTitle(editTitleField.getText().trim());
        transaction.setAmount(Double.parseDouble(editAmountField.getText().trim()));
        transaction.setType((String) editTypeComboBox.getSelectedItem());
        transaction.setPaymentMethod(PaymentMethod.valueOf(
            (String) editPaymentMethodComboBox.getSelectedItem()));
        transaction.setCategory(editCategoryField.getText().trim());
        transaction.setDescription(editDescriptionArea.getText().trim());
        
        // Procesar etiquetas
        String tagsText = editTagsField.getText().trim();
        if (!tagsText.isEmpty()) {
            List<String> tags = Arrays.asList(tagsText.split("\\s*,\\s*"));
            transaction.setTags(tags);
        } else {
            transaction.setTags(new ArrayList<>());
        }
        
        // Mantener la fecha original o actualizarla a la actual
        transaction.setDate(ZonedDateTime.now(ZoneId.systemDefault()));
    }
    
    /**
     * Limpia el formulario de creación
     */
    private void clearCreateForm() {
        createTitleField.setText("");
        createAmountField.setText("");
        createTypeComboBox.setSelectedIndex(0);
        createPaymentMethodComboBox.setSelectedIndex(0);
        createCategoryField.setText("");
        createDescriptionArea.setText("");
        createTagsField.setText("");
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
            TabbedTransactionClientGUI gui = new TabbedTransactionClientGUI();
            gui.setVisible(true);
        });
    }
}