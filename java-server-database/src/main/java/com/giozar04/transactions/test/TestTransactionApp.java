package com.giozar04.transactions.test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import com.giozar04.databases.domain.interfaces.DatabaseConnectionInterface;
import com.giozar04.databases.infrastructure.repositories.DatabaseConnectionMySQL;
import com.giozar04.transactions.application.services.TransactionService;
import com.giozar04.transactions.domain.entities.Transaction;
import com.giozar04.transactions.domain.enums.PaymentMethod;
import com.giozar04.transactions.infrastructure.repositories.TransactionRepositoryMySQL;

/**
 * Aplicación para probar la funcionalidad de creación y recuperación de transacciones
 * usando la base de datos MySQL.
 */
public class TestTransactionApp {

    // Configuración de la base de datos
    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "finanzas";  // Cambia esto según el nombre de tu base de datos
    private static final String DB_USER = "giovanni";  // Cambia esto según tu usuario
    private static final String DB_PASSWORD = "finanzas123";  // Cambia esto según tu contraseña

    public static void main(String[] args) {
        System.out.println("Iniciando prueba de transacciones con MySQL...");
        
        try {
            // Inicializar conexión a la base de datos
            DatabaseConnectionInterface dbConnection = DatabaseConnectionMySQL.getInstance(
                DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD);
            
            // Conectar a la base de datos
            System.out.println("Conectando a la base de datos...");
            dbConnection.connect();
            
            // Crear el repositorio y servicio
            TransactionRepositoryMySQL transactionRepo = new TransactionRepositoryMySQL(dbConnection);
            TransactionService transactionService = new TransactionService(transactionRepo);
            
            // Mostrar menú de opciones
            Scanner scanner = new Scanner(System.in);
            boolean exit = false;
            
            while (!exit) {
                System.out.println("\n===== MENÚ DE PRUEBA DE TRANSACCIONES =====");
                System.out.println("1. Crear transacción de prueba");
                System.out.println("2. Obtener todas las transacciones");
                System.out.println("3. Buscar transacción por ID");
                System.out.println("4. Actualizar transacción por ID");
                System.out.println("5. Eliminar transacción por ID");
                System.out.println("0. Salir");
                System.out.print("Seleccione una opción: ");
                
                int option = scanner.nextInt();
                scanner.nextLine(); // Consumir nueva línea
                
                switch (option) {
                    case 1:
                        createTestTransaction(transactionService);
                        break;
                    case 2:
                        getAllTransactions(transactionService);
                        break;
                    case 3:
                        getTransactionById(transactionService, scanner);
                        break;
                    case 4:
                        updateTransaction(transactionService, scanner);
                        break;
                    case 5:
                        deleteTransaction(transactionService, scanner);
                        break;
                    case 0:
                        exit = true;
                        break;
                    default:
                        System.out.println("Opción no válida.");
                }
            }
            
            // Cerrar la conexión
            dbConnection.disconnect();
            scanner.close();
            System.out.println("Prueba finalizada.");
            
        } catch (Exception e) {
            System.err.println("Error durante la prueba: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Crea una transacción de prueba y la guarda en la base de datos.
     */
    private static void createTestTransaction(TransactionService service) {
        try {
            // Crear una transacción de prueba
            Transaction transaction = new Transaction();
            transaction.setType("EXPENSE");
            transaction.setPaymentMethod(PaymentMethod.CARD_CREDIT);
            transaction.setAmount(1500.75);
            transaction.setTitle("Compra Supermercado");
            transaction.setCategory("Alimentación");
            transaction.setDescription("Compras semanales");
            transaction.setComments("Incluye productos de limpieza");
            transaction.setDate(ZonedDateTime.now(ZoneId.of("America/Mexico_City")));
            
            // Asignar etiquetas
            List<String> tags = Arrays.asList("Necesario", "Mensual", "Hogar");
            transaction.setTags(tags);
            
            // Guardar en la base de datos
            Transaction savedTransaction = service.createTransaction(transaction);
            
            System.out.println("Transacción creada exitosamente con ID: " + savedTransaction.getId());
            printTransactionDetails(savedTransaction);
            
        } catch (Exception e) {
            System.err.println("Error al crear transacción: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Obtiene todas las transacciones de la base de datos.
     */
    private static void getAllTransactions(TransactionService service) {
        try {
            List<Transaction> transactions = service.getAllTransactions();
            
            if (transactions.isEmpty()) {
                System.out.println("No hay transacciones en la base de datos.");
                return;
            }
            
            System.out.println("\n===== LISTA DE TRANSACCIONES =====");
            System.out.println("Total de transacciones: " + transactions.size());
            
            for (Transaction transaction : transactions) {
                System.out.println("\n----- Transacción ID: " + transaction.getId() + " -----");
                printTransactionDetails(transaction);
            }
            
        } catch (Exception e) {
            System.err.println("Error al obtener transacciones: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Busca una transacción por ID.
     */
    private static void getTransactionById(TransactionService service, Scanner scanner) {
        try {
            System.out.print("Ingrese el ID de la transacción: ");
            long id = scanner.nextLong();
            scanner.nextLine(); // Consumir nueva línea
            
            Transaction transaction = service.getTransactionById(id);
            
            System.out.println("\n===== DETALLE DE TRANSACCIÓN =====");
            printTransactionDetails(transaction);
            
        } catch (Exception e) {
            System.err.println("Error al obtener transacción: " + e.getMessage());
        }
    }
    
    /**
     * Actualiza una transacción existente por ID.
     */
    private static void updateTransaction(TransactionService service, Scanner scanner) {
        try {
            System.out.print("Ingrese el ID de la transacción a actualizar: ");
            long id = scanner.nextLong();
            scanner.nextLine(); // Consumir nueva línea
            
            // Primero obtenemos la transacción existente
            Transaction existingTransaction = service.getTransactionById(id);
            
            System.out.println("\nTransacción actual:");
            printTransactionDetails(existingTransaction);
            
            // Solicitar nueva información
            System.out.println("\nIngrese los nuevos datos (presione Enter para mantener valor actual):");
            
            System.out.print("Título [" + existingTransaction.getTitle() + "]: ");
            String title = scanner.nextLine();
            if (!title.isEmpty()) {
                existingTransaction.setTitle(title);
            }
            
            System.out.print("Monto [" + existingTransaction.getAmount() + "]: ");
            String amountStr = scanner.nextLine();
            if (!amountStr.isEmpty()) {
                existingTransaction.setAmount(Double.parseDouble(amountStr));
            }
            
            System.out.print("Descripción [" + existingTransaction.getDescription() + "]: ");
            String description = scanner.nextLine();
            if (!description.isEmpty()) {
                existingTransaction.setDescription(description);
            }
            
            // Solicitar método de pago
            System.out.println("Método de pago actual: " + existingTransaction.getPaymentMethod());
            System.out.println("Opciones de método de pago:");
            System.out.println("1. CASH");
            System.out.println("2. CARD_DEBIT");
            System.out.println("3. CARD_CREDIT");
            System.out.print("Seleccione nueva opción (0 para mantener actual): ");
            int paymentOption = scanner.nextInt();
            scanner.nextLine();
            
            if (paymentOption > 0) {
                switch (paymentOption) {
                    case 1:
                        existingTransaction.setPaymentMethod(PaymentMethod.CASH);
                        break;
                    case 2:
                        existingTransaction.setPaymentMethod(PaymentMethod.CARD_DEBIT);
                        break;
                    case 3:
                        existingTransaction.setPaymentMethod(PaymentMethod.CARD_CREDIT);
                        break;
                    default:
                        System.out.println("Opción inválida, se mantiene el método de pago actual.");
                }
            }
            
            // Guardar cambios
            Transaction updatedTransaction = service.updateTransactionById(id, existingTransaction);
            
            System.out.println("\nTransacción actualizada exitosamente:");
            printTransactionDetails(updatedTransaction);
            
        } catch (Exception e) {
            System.err.println("Error al actualizar transacción: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Elimina una transacción por ID.
     */
    private static void deleteTransaction(TransactionService service, Scanner scanner) {
        try {
            System.out.print("Ingrese el ID de la transacción a eliminar: ");
            long id = scanner.nextLong();
            scanner.nextLine(); // Consumir nueva línea
            
            // Confirmar eliminación
            System.out.print("¿Está seguro de eliminar la transacción? (s/n): ");
            String confirm = scanner.nextLine();
            
            if (confirm.equalsIgnoreCase("s")) {
                service.deleteTransactionById(id);
                System.out.println("Transacción con ID " + id + " eliminada exitosamente.");
            } else {
                System.out.println("Eliminación cancelada.");
            }
            
        } catch (Exception e) {
            System.err.println("Error al eliminar transacción: " + e.getMessage());
        }
    }
    
    /**
     * Imprime los detalles de una transacción.
     */
    private static void printTransactionDetails(Transaction transaction) {
        System.out.println("ID: " + transaction.getId());
        System.out.println("Tipo: " + transaction.getType());
        System.out.println("Método de pago: " + transaction.getPaymentMethod());
        System.out.println("Monto: " + transaction.getAmount());
        System.out.println("Título: " + transaction.getTitle());
        System.out.println("Categoría: " + transaction.getCategory());
        System.out.println("Descripción: " + transaction.getDescription());
        System.out.println("Comentarios: " + transaction.getComments());
        System.out.println("Fecha: " + transaction.getDate());
        System.out.println("Zona horaria: " + transaction.getDate().getZone());
        System.out.println("Etiquetas: " + (transaction.getTags() != null ? String.join(", ", transaction.getTags()) : "Ninguna"));
    }
}