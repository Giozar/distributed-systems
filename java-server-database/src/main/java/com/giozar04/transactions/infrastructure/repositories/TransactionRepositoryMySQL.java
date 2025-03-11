package com.giozar04.transactions.infrastructure.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.giozar04.databases.domain.interfaces.DatabaseConnectionInterface;
import com.giozar04.transactions.domain.classes.TransactionExceptions;
import com.giozar04.transactions.domain.entities.Transaction;
import com.giozar04.transactions.domain.models.TransactionRepository;

/**
 * Implementación MySQL del repositorio de transacciones.
 * Maneja operaciones CRUD para entidades Transaction en una base de datos MySQL.
 */
public class TransactionRepositoryMySQL extends TransactionRepository {

    // Consultas SQL como constantes para mejor mantenibilidad
    private static final String SQL_INSERT = "INSERT INTO transactions (amount, description, date) VALUES (?, ?, ?)";
    private static final String SQL_SELECT_BY_ID = "SELECT * FROM transactions WHERE id = ?";
    private static final String SQL_UPDATE = "UPDATE transactions SET amount = ?, description = ?, date = ? WHERE id = ?";
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
                statement.setDouble(1, transaction.getAmount());
                statement.setString(2, transaction.getDescription());
                statement.setTimestamp(3, java.sql.Timestamp.valueOf(
                        transaction.getDate().toLocalDateTime()));
                
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
                statement.setDouble(1, transaction.getAmount());
                statement.setString(2, transaction.getDescription());
                statement.setTimestamp(3, java.sql.Timestamp.valueOf(
                        transaction.getDate().toLocalDateTime()));
                statement.setLong(4, id);
                
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
        transaction.setAmount(resultSet.getDouble("amount"));
        transaction.setDescription(resultSet.getString("description"));
        
        // Manejo seguro de la fecha
        java.sql.Timestamp timestamp = resultSet.getTimestamp("date");
        LocalDateTime dateTime = timestamp != null 
            ? timestamp.toLocalDateTime() 
            : LocalDateTime.now();
        transaction.setDate(dateTime.atZone(ZoneId.systemDefault()));
        
        return transaction;
    }
}