package com.giozar04.transactions.infrastructure.handlers;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.giozar04.servers.domain.handlers.MessageHandler;
import com.giozar04.servers.domain.models.ClientSocket;
import com.giozar04.servers.domain.models.Message;
import com.giozar04.shared.logging.CustomLogger;
import com.giozar04.transactions.application.services.TransactionService;
import com.giozar04.transactions.domain.entities.Transaction;

/**
 * Clase que proporciona manejadores para las operaciones relacionadas con transacciones.
 * Utiliza serialización nativa de Java en lugar de bibliotecas externas.
 */
public class TransactionHandlers {
    
    private static final CustomLogger LOGGER = new CustomLogger();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_ZONED_DATE_TIME;
    
    /**
     * Tipos de mensajes para operaciones de transacciones.
     */
    public static final class MessageTypes {
        public static final String CREATE_TRANSACTION = "CREATE_TRANSACTION";
        public static final String GET_TRANSACTION = "GET_TRANSACTION";
        public static final String UPDATE_TRANSACTION = "UPDATE_TRANSACTION";
        public static final String DELETE_TRANSACTION = "DELETE_TRANSACTION";
        public static final String GET_ALL_TRANSACTIONS = "GET_ALL_TRANSACTIONS";
    }
    
    /**
     * Convierte una transacción a un mapa de propiedades.
     * 
     * @param transaction La transacción a convertir.
     * @return Un mapa con las propiedades de la transacción.
     */
    private static Map<String, Object> transactionToMap(Transaction transaction) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", transaction.getId());
        map.put("amount", transaction.getAmount());
        map.put("description", transaction.getDescription());
        map.put("date", transaction.getDate().format(DATE_FORMATTER));
        return map;
    }
    
    /**
     * Convierte un mapa de propiedades a una transacción.
     * 
     * @param map El mapa con las propiedades.
     * @return Una transacción con los valores del mapa.
     */
    private static Transaction mapToTransaction(Map<String, Object> map) {
        Transaction transaction = new Transaction();
        
        if (map.containsKey("id")) {
            transaction.setId(((Number) map.get("id")).longValue());
        }
        
        if (map.containsKey("amount")) {
            transaction.setAmount(((Number) map.get("amount")).doubleValue());
        }
        
        if (map.containsKey("description")) {
            transaction.setDescription((String) map.get("description"));
        }
        
        if (map.containsKey("date")) {
            transaction.setDate(ZonedDateTime.parse((String) map.get("date"), DATE_FORMATTER));
        } else {
            transaction.setDate(ZonedDateTime.now());
        }
        
        return transaction;
    }
    
    /**
     * Crea un manejador para la creación de transacciones.
     *
     * @param transactionService El servicio de transacciones a utilizar.
     * @return Un manejador para mensajes de creación de transacciones.
     */
    public static MessageHandler createTransactionHandler(TransactionService transactionService) {
        return (ClientSocket clientSocket, Message message) -> {
            LOGGER.info("Procesando solicitud de creación de transacción");
            
            // Extraer datos de la transacción del mensaje
            Map<String, Object> transactionData = (Map<String, Object>) message.getData("transaction");
            if (transactionData == null) {
                return Message.createErrorMessage(MessageTypes.CREATE_TRANSACTION, 
                        "Datos de transacción no proporcionados");
            }
            
            // Convertir mapa a objeto Transaction
            Transaction transaction = mapToTransaction(transactionData);
            
            // Crear la transacción usando el servicio
            Transaction createdTransaction = transactionService.createTransaction(transaction);
            
            // Crear mensaje de respuesta
            Message response = Message.createSuccessMessage(
                    MessageTypes.CREATE_TRANSACTION, 
                    "Transacción creada exitosamente");
            
            // Convertir la transacción creada a un mapa para incluirla en la respuesta
            response.addData("transaction", transactionToMap(createdTransaction));
            
            return response;
        };
    }
    
    /**
     * Crea un manejador para obtener una transacción por ID.
     *
     * @param transactionService El servicio de transacciones a utilizar.
     * @return Un manejador para mensajes de obtención de transacciones.
     */
    public static MessageHandler getTransactionHandler(TransactionService transactionService) {
        return (ClientSocket clientSocket, Message message) -> {
            LOGGER.info("Procesando solicitud de obtención de transacción");
            
            // Extraer ID de la transacción del mensaje
            Long id = (Long) message.getData("id");
            if (id == null) {
                return Message.createErrorMessage(MessageTypes.GET_TRANSACTION, 
                        "ID de transacción no proporcionado");
            }
            
            // Obtener la transacción usando el servicio
            Transaction transaction = transactionService.getTransactionById(id);
            
            // Crear mensaje de respuesta
            Message response = Message.createSuccessMessage(
                    MessageTypes.GET_TRANSACTION, 
                    "Transacción obtenida exitosamente");
            
            // Convertir la transacción a un mapa para incluirla en la respuesta
            response.addData("transaction", transactionToMap(transaction));
            
            return response;
        };
    }
    
    /**
     * Crea un manejador para actualizar una transacción.
     *
     * @param transactionService El servicio de transacciones a utilizar.
     * @return Un manejador para mensajes de actualización de transacciones.
     */
    public static MessageHandler updateTransactionHandler(TransactionService transactionService) {
        return (ClientSocket clientSocket, Message message) -> {
            LOGGER.info("Procesando solicitud de actualización de transacción");
            
            // Extraer ID de la transacción del mensaje
            Long id = (Long) message.getData("id");
            if (id == null) {
                return Message.createErrorMessage(MessageTypes.UPDATE_TRANSACTION, 
                        "ID de transacción no proporcionado");
            }
            
            // Extraer datos de la transacción del mensaje
            Map<String, Object> transactionData = (Map<String, Object>) message.getData("transaction");
            if (transactionData == null) {
                return Message.createErrorMessage(MessageTypes.UPDATE_TRANSACTION, 
                        "Datos de transacción no proporcionados");
            }
            
            // Convertir mapa a objeto Transaction
            Transaction transaction = mapToTransaction(transactionData);
            
            // Actualizar la transacción usando el servicio
            Transaction updatedTransaction = transactionService.updateTransactionById(id, transaction);
            
            // Crear mensaje de respuesta
            Message response = Message.createSuccessMessage(
                    MessageTypes.UPDATE_TRANSACTION, 
                    "Transacción actualizada exitosamente");
            
            // Convertir la transacción actualizada a un mapa para incluirla en la respuesta
            response.addData("transaction", transactionToMap(updatedTransaction));
            
            return response;
        };
    }
    
    /**
     * Crea un manejador para eliminar una transacción.
     *
     * @param transactionService El servicio de transacciones a utilizar.
     * @return Un manejador para mensajes de eliminación de transacciones.
     */
    public static MessageHandler deleteTransactionHandler(TransactionService transactionService) {
        return (ClientSocket clientSocket, Message message) -> {
            LOGGER.info("Procesando solicitud de eliminación de transacción");
            
            // Extraer ID de la transacción del mensaje
            Long id = (Long) message.getData("id");
            if (id == null) {
                return Message.createErrorMessage(MessageTypes.DELETE_TRANSACTION, 
                        "ID de transacción no proporcionado");
            }
            
            // Eliminar la transacción usando el servicio
            transactionService.deleteTransactionById(id);
            
            // Crear mensaje de respuesta
            return Message.createSuccessMessage(
                    MessageTypes.DELETE_TRANSACTION, 
                    "Transacción eliminada exitosamente");
        };
    }
    
    /**
     * Crea un manejador para obtener todas las transacciones.
     *
     * @param transactionService El servicio de transacciones a utilizar.
     * @return Un manejador para mensajes de obtención de todas las transacciones.
     */
    public static MessageHandler getAllTransactionsHandler(TransactionService transactionService) {
        return (ClientSocket clientSocket, Message message) -> {
            LOGGER.info("Procesando solicitud de obtención de todas las transacciones");
            
            // Obtener todas las transacciones usando el servicio
            List<Transaction> transactions = transactionService.getAllTransactions();
            
            // Crear mensaje de respuesta
            Message response = Message.createSuccessMessage(
                    MessageTypes.GET_ALL_TRANSACTIONS, 
                    "Transacciones obtenidas exitosamente");
            
            // Convertir cada transacción a un mapa y añadirlas a una lista
            Map<String, Object>[] transactionMaps = new Map[transactions.size()];
            for (int i = 0; i < transactions.size(); i++) {
                transactionMaps[i] = transactionToMap(transactions.get(i));
            }
            
            response.addData("transactions", transactionMaps);
            response.addData("count", transactions.size());
            
            return response;
        };
    }
}