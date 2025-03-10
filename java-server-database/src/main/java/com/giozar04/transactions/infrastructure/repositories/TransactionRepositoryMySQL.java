package com.giozar04.transactions.infrastructure.repositories;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.giozar04.shared.CustomLogger;
import com.giozar04.transactions.domain.classes.TransactionExceptions;
import com.giozar04.transactions.domain.entities.Transaction;
import com.giozar04.transactions.domain.interfaces.TransactionRepository;

/**
 * Implementación MySQL del repositorio de transacciones.
 * Maneja operaciones CRUD para entidades Transaction en una base de datos MySQL.
 */
public class TransactionRepositoryMySQL implements TransactionRepository {

    private final Connection connection;
    private final CustomLogger customLogger;
    
    // Consultas SQL como constantes para mejor mantenibilidad
    private static final String SQL_INSERT = "INSERT INTO transactions (amount, description, date) VALUES (?, ?, ?)";
    private static final String SQL_SELECT_BY_ID = "SELECT * FROM transactions WHERE id = ?";
    private static final String SQL_UPDATE = "UPDATE transactions SET amount = ?, description = ?, date = ? WHERE id = ?";
    private static final String SQL_DELETE = "DELETE FROM transactions WHERE id = ?";
    private static final String SQL_SELECT_ALL = "SELECT * FROM transactions ORDER BY date DESC";

    /**
     * Constructor que inicializa la conexión a la base de datos.
     *
     * @param connection la conexión a la base de datos MySQL
     */
    public TransactionRepositoryMySQL(Connection connection) {
        this.connection = Objects.requireNonNull(connection, "La conexión no puede ser nula");
        this.customLogger = new CustomLogger();
    }

    @Override
    public Transaction createTransaction(Transaction transaction) {
        Objects.requireNonNull(transaction, "La transacción no puede ser nula");
        
        try (PreparedStatement statement = connection.prepareStatement(SQL_INSERT, PreparedStatement.RETURN_GENERATED_KEYS)) {
            statement.setDouble(1, transaction.getAmount());
            statement.setString(2, transaction.getDescription());
            statement.setDate(3, java.sql.Date.valueOf(transaction.getDate().toLocalDate()));
            
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
            
            customLogger.info("Transacción creada exitosamente con ID: " + transaction.getId());
            return transaction;
        } catch (SQLException e) {
            customLogger.error("Error al crear una transacción: " + e.getMessage(), e);
            throw new TransactionExceptions.TransactionCreationException("Error al crear una transacción", e);
        }
    }

    @Override
    public Transaction getTransactionById(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor que cero");
        }
        
        try (PreparedStatement statement = connection.prepareStatement(SQL_SELECT_BY_ID)) {
            statement.setLong(1, id);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Transaction transaction = mapResultSetToTransaction(resultSet);
                    customLogger.info("Transacción obtenida exitosamente con ID: " + id);
                    return transaction;
                } else {
                    customLogger.warn("Transacción no encontrada con ID: " + id, null);
                    throw new TransactionExceptions.TransactionNotFoundException("Transacción no encontrada con ID: " + id, null);
                }
            }
        } catch (SQLException e) {
            customLogger.error("Error al obtener la transacción con ID: " + id, e);
            throw new TransactionExceptions.TransactionNotFoundException("Error al obtener la transacción con ID: " + id, e);
        }
    }

    @Override
    public Transaction updateTransactionById(long id, Transaction transaction) {
        Objects.requireNonNull(transaction, "La transacción no puede ser nula");
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor que cero");
        }
        
        try (PreparedStatement statement = connection.prepareStatement(SQL_UPDATE)) {
            statement.setDouble(1, transaction.getAmount());
            statement.setString(2, transaction.getDescription());
            statement.setDate(3, java.sql.Date.valueOf(transaction.getDate().toLocalDate()));
            statement.setLong(4, id);
            
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                customLogger.warn("Transacción no encontrada con ID: " + id, null);
                throw new TransactionExceptions.TransactionNotFoundException("Transacción no encontrada con ID: " + id, null);
            }
            
            // Actualizar el ID en el objeto
            transaction.setId(id);
            
            customLogger.info("Transacción actualizada exitosamente con ID: " + id);
            return transaction;
        } catch (SQLException e) {
            customLogger.error("Error al actualizar la transacción con ID: " + id, e);
            throw new TransactionExceptions.TransactionUpdateException("Error al actualizar la transacción con ID: " + id, e);
        }
    }

    @Override
    public void deleteTransactionById(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor que cero");
        }
        
        try (PreparedStatement statement = connection.prepareStatement(SQL_DELETE)) {
            statement.setLong(1, id);
            
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                customLogger.warn("Transacción no encontrada con ID: " + id, null);
                throw new TransactionExceptions.TransactionNotFoundException("Transacción no encontrada con ID: " + id, null);
            }
            
            customLogger.info("Transacción eliminada exitosamente con ID: " + id);
        } catch (SQLException e) {
            customLogger.error("Error al eliminar la transacción con ID: " + id, e);
            throw new TransactionExceptions.TransactionDeletionException("Error al eliminar la transacción con ID: " + id, e);
        }
    }

    @Override
    public List<Transaction> getAllTransactions() {
        List<Transaction> transactions = new ArrayList<>();
        
        try (PreparedStatement statement = connection.prepareStatement(SQL_SELECT_ALL);
             ResultSet resultSet = statement.executeQuery()) {
            
            while (resultSet.next()) {
                Transaction transaction = mapResultSetToTransaction(resultSet);
                transactions.add(transaction);
            }
            
            customLogger.info("Se obtuvieron " + transactions.size() + " transacciones exitosamente");
            return transactions;
        } catch (SQLException e) {
            customLogger.error("Error al obtener todas las transacciones", e);
            throw new TransactionExceptions.TransactionRetrievalException("Error al obtener todas las transacciones", e);
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
        java.sql.Date sqlDate = resultSet.getDate("date");
        LocalDateTime dateTime = sqlDate != null 
            ? sqlDate.toLocalDate().atStartOfDay() 
            : LocalDateTime.now();
        transaction.setDate(dateTime.atZone(java.time.ZoneId.systemDefault()));
        
        return transaction;
    }
}