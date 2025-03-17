package com.giozar04.transactions.infrastructure.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.giozar04.databases.domain.interfaces.DatabaseConnectionInterface;
import com.giozar04.transactions.domain.classes.TransactionExceptions;
import com.giozar04.transactions.domain.entities.Transaction;
import com.giozar04.transactions.domain.enums.PaymentMethod;
import com.giozar04.transactions.domain.models.TransactionRepositoryAbstract;

/**
 * Implementación MySQL del repositorio de transacciones.
 * Maneja operaciones CRUD para entidades Transaction en una base de datos MySQL.
 */
public class TransactionRepositoryMySQL extends TransactionRepositoryAbstract {

    // Consultas SQL como constantes para mejor mantenibilidad
    private static final String SQL_INSERT = "INSERT INTO transactions (type, payment_method, amount, title, category, description, comments, date, timezone, tags) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_SELECT_BY_ID = "SELECT * FROM transactions WHERE id = ?";
    private static final String SQL_UPDATE = "UPDATE transactions SET type = ?, payment_method = ?, amount = ?, title = ?, category = ?, description = ?, comments = ?, date = ?, timezone = ?, tags = ? WHERE id = ?";
    private static final String SQL_DELETE = "DELETE FROM transactions WHERE id = ?";
    private static final String SQL_SELECT_ALL = "SELECT * FROM transactions ORDER BY date DESC";

    /**
     * Constructor que inicializa el repositorio con una conexión a la base de datos.
     *
     * @param databaseConnection la conexión a la base de datos
     */
    public TransactionRepositoryMySQL(DatabaseConnectionInterface databaseConnection) {
        super(databaseConnection);
    }

    @Override
    public Transaction createTransaction(Transaction transaction) {
        // Validar la transacción usando el método de la clase base
        validateTransaction(transaction);
        
        Connection connection = null;
        
        try {
            // Obtener conexión de la interfaz
            connection = databaseConnection.getConnection();
            
            try (PreparedStatement statement = connection.prepareStatement(SQL_INSERT, PreparedStatement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, transaction.getType());
                statement.setString(2, transaction.getPaymentMethod().name());
                statement.setDouble(3, transaction.getAmount());
                statement.setString(4, transaction.getTitle());
                statement.setString(5, transaction.getCategory());
                statement.setString(6, transaction.getDescription());
                statement.setString(7, transaction.getComments());
                
                // Convertir ZonedDateTime a Timestamp para almacenamiento
                LocalDateTime localDateTime = transaction.getDate().toLocalDateTime();
                statement.setTimestamp(8, Timestamp.valueOf(localDateTime));
                
                // Guardar la zona horaria
                statement.setString(9, transaction.getDate().getZone().getId());
                
                // Convertir lista de tags a string separado por comas
                statement.setString(10, transaction.getTagsAsString());
                
                int affectedRows = statement.executeUpdate();
                if (affectedRows == 0) {
                    throw new SQLException("La creación de la transacción falló, ninguna fila afectada.");
                }
                
                // Obtener el ID generado
                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        transaction.setId(generatedKeys.getLong(1));
                    } else {
                        throw new SQLException("La creación de la transacción falló, no se obtuvo el ID.");
                    }
                }
                
                // Confirmar la transacción
                databaseConnection.commitTransaction();
                
                logger.info("Transacción creada exitosamente con ID: " + transaction.getId());
                return transaction;
            } catch (SQLException e) {
                // Revertir la transacción en caso de error
                try {
                    databaseConnection.rollbackTransaction();
                } catch (SQLException ex) {
                    logger.error("Error al revertir la transacción: " + ex.getMessage(), ex);
                }
                
                logger.error("Error al crear una transacción: " + e.getMessage(), e);
                throw new TransactionExceptions.TransactionCreationException("Error al crear una transacción", e);
            }
        } catch (SQLException e) {
            logger.error("Error al obtener la conexión a la base de datos: " + e.getMessage(), e);
            throw new TransactionExceptions.TransactionCreationException("Error al obtener la conexión a la base de datos", e);
        }
    }

    @Override
    public Transaction getTransactionById(long id) {
        // Validar el ID usando el método de la clase base
        validateId(id);
        
        Connection connection = null;
        
        try {
            // Obtener conexión de la interfaz
            connection = databaseConnection.getConnection();
            
            try (PreparedStatement statement = connection.prepareStatement(SQL_SELECT_BY_ID)) {
                statement.setLong(1, id);
                
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        Transaction transaction = mapResultSetToTransaction(resultSet);
                        logger.info("Transacción obtenida exitosamente con ID: " + id);
                        return transaction;
                    } else {
                        logger.warn("Transacción no encontrada con ID: " + id, null);
                        throw new TransactionExceptions.TransactionNotFoundException("Transacción no encontrada con ID: " + id, null);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Error al obtener la transacción con ID: " + id, e);
            throw new TransactionExceptions.TransactionNotFoundException("Error al obtener la transacción con ID: " + id, e);
        }
    }

    @Override
    public Transaction updateTransactionById(long id, Transaction transaction) {
        // Validar el ID y la transacción usando los métodos de la clase base
        validateId(id);
        validateTransaction(transaction);
        
        Connection connection = null;
        
        try {
            // Obtener conexión de la interfaz
            connection = databaseConnection.getConnection();
            
            try (PreparedStatement statement = connection.prepareStatement(SQL_UPDATE)) {
                statement.setString(1, transaction.getType());
                statement.setString(2, transaction.getPaymentMethod().name());
                statement.setDouble(3, transaction.getAmount());
                statement.setString(4, transaction.getTitle());
                statement.setString(5, transaction.getCategory());
                statement.setString(6, transaction.getDescription());
                statement.setString(7, transaction.getComments());
                
                // Convertir ZonedDateTime a Timestamp para almacenamiento
                LocalDateTime localDateTime = transaction.getDate().toLocalDateTime();
                statement.setTimestamp(8, Timestamp.valueOf(localDateTime));
                
                // Guardar la zona horaria
                statement.setString(9, transaction.getDate().getZone().getId());
                
                // Convertir lista de tags a string separado por comas
                statement.setString(10, transaction.getTagsAsString());
                
                statement.setLong(11, id);
                
                int affectedRows = statement.executeUpdate();
                if (affectedRows == 0) {
                    logger.warn("Transacción no encontrada con ID: " + id, null);
                    throw new TransactionExceptions.TransactionNotFoundException("Transacción no encontrada con ID: " + id, null);
                }
                
                // Confirmar la transacción
                databaseConnection.commitTransaction();
                
                // Actualizar el ID en el objeto
                transaction.setId(id);
                
                logger.info("Transacción actualizada exitosamente con ID: " + id);
                return transaction;
            } catch (SQLException e) {
                // Revertir la transacción en caso de error
                try {
                    databaseConnection.rollbackTransaction();
                } catch (SQLException ex) {
                    logger.error("Error al revertir la transacción: " + ex.getMessage(), ex);
                }
                
                logger.error("Error al actualizar la transacción con ID: " + id, e);
                throw new TransactionExceptions.TransactionUpdateException("Error al actualizar la transacción con ID: " + id, e);
            }
        } catch (SQLException e) {
            logger.error("Error al obtener la conexión a la base de datos: " + e.getMessage(), e);
            throw new TransactionExceptions.TransactionUpdateException("Error al obtener la conexión a la base de datos", e);
        }
    }

    @Override
    public void deleteTransactionById(long id) {
        // Validar el ID usando el método de la clase base
        validateId(id);
        
        Connection connection = null;
        
        try {
            // Obtener conexión de la interfaz
            connection = databaseConnection.getConnection();
            
            try (PreparedStatement statement = connection.prepareStatement(SQL_DELETE)) {
                statement.setLong(1, id);
                
                int affectedRows = statement.executeUpdate();
                if (affectedRows == 0) {
                    logger.warn("Transacción no encontrada con ID: " + id, null);
                    throw new TransactionExceptions.TransactionNotFoundException("Transacción no encontrada con ID: " + id, null);
                }
                
                // Confirmar la transacción
                databaseConnection.commitTransaction();
                
                logger.info("Transacción eliminada exitosamente con ID: " + id);
            } catch (SQLException e) {
                // Revertir la transacción en caso de error
                try {
                    databaseConnection.rollbackTransaction();
                } catch (SQLException ex) {
                    logger.error("Error al revertir la transacción: " + ex.getMessage(), ex);
                }
                
                logger.error("Error al eliminar la transacción con ID: " + id, e);
                throw new TransactionExceptions.TransactionDeletionException("Error al eliminar la transacción con ID: " + id, e);
            }
        } catch (SQLException e) {
            logger.error("Error al obtener la conexión a la base de datos: " + e.getMessage(), e);
            throw new TransactionExceptions.TransactionDeletionException("Error al obtener la conexión a la base de datos", e);
        }
    }

    @Override
    public List<Transaction> getAllTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        
        Connection connection = null;
        
        try {
            // Obtener conexión de la interfaz
            connection = databaseConnection.getConnection();
            
            try (PreparedStatement statement = connection.prepareStatement(SQL_SELECT_ALL);
                 ResultSet resultSet = statement.executeQuery()) {
                
                while (resultSet.next()) {
                    Transaction transaction = mapResultSetToTransaction(resultSet);
                    transactions.add(transaction);
                }
                
                logger.info("Se obtuvieron " + transactions.size() + " transacciones exitosamente");
                return transactions;
            } catch (SQLException e) {
                logger.error("Error al obtener todas las transacciones", e);
                throw new TransactionExceptions.TransactionRetrievalException("Error al obtener todas las transacciones", e);
            }
        } catch (SQLException e) {
            logger.error("Error al obtener la conexión a la base de datos: " + e.getMessage(), e);
            throw new TransactionExceptions.TransactionRetrievalException("Error al obtener la conexión a la base de datos", e);
        }
    }
    
    /**
     * Convierte un ResultSet en un objeto Transaction.
     *
     * @param resultSet el ResultSet que contiene los datos de la transacción
     * @return un objeto Transaction con los datos del ResultSet
     * @throws SQLException si ocurre un error al acceder a los datos del ResultSet
     */
    private Transaction mapResultSetToTransaction(ResultSet resultSet) throws SQLException {
        Transaction transaction = new Transaction();
        transaction.setId(resultSet.getLong("id"));
        transaction.setType(resultSet.getString("type"));
        
        // Convertir string a enum
        String paymentMethodStr = resultSet.getString("payment_method");
        try {
            transaction.setPaymentMethod(PaymentMethod.valueOf(paymentMethodStr));
        } catch (IllegalArgumentException e) {
            logger.warn("Método de pago desconocido: " + paymentMethodStr + ". Utilizando valor por defecto CASH.", e);
            transaction.setPaymentMethod(PaymentMethod.CASH);
        }
        
        transaction.setAmount(resultSet.getDouble("amount"));
        transaction.setTitle(resultSet.getString("title"));
        transaction.setCategory(resultSet.getString("category"));
        transaction.setDescription(resultSet.getString("description"));
        transaction.setComments(resultSet.getString("comments"));
        
        // Reconstruir ZonedDateTime a partir de timestamp y zona horaria
        Timestamp timestamp = resultSet.getTimestamp("date");
        String timezone = resultSet.getString("timezone");
        if (timestamp != null) {
            LocalDateTime localDateTime = timestamp.toLocalDateTime();
            ZoneId zoneId = ZoneId.of(timezone != null && !timezone.isEmpty() ? timezone : "UTC");
            transaction.setDate(ZonedDateTime.of(localDateTime, zoneId));
        } else {
            transaction.setDate(ZonedDateTime.now());
        }
        
        // Procesar tags
        String tagsString = resultSet.getString("tags");
        transaction.setTagsFromString(tagsString);
        
        return transaction;
    }
}