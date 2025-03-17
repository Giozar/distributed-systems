package com.giozar04.transactions.domain.models;

import java.util.List;
import java.util.Objects;

import com.giozar04.databases.domain.interfaces.DatabaseConnectionInterface;
import com.giozar04.shared.logging.CustomLogger;
import com.giozar04.transactions.domain.entities.Transaction;
import com.giozar04.transactions.domain.interfaces.TransactionRepositoryInterface;

/**
 * Clase base abstracta para los repositorios de transacciones.
 * Proporciona funcionalidad común para todas las implementaciones de repositorios.
 */
public abstract class TransactionRepositoryAbstract implements TransactionRepositoryInterface {
    
    // Conexión a la base de datos (inyectada mediante constructor)
    protected final DatabaseConnectionInterface databaseConnection;
    
    // Logger para registro de eventos
    protected final CustomLogger logger;
    
    /**
     * Constructor que inicializa la conexión a la base de datos.
     *
     * @param databaseConnection la conexión a la base de datos
     * @throws NullPointerException si databaseConnection es null
     */
    protected TransactionRepositoryAbstract(DatabaseConnectionInterface databaseConnection) {
        this.databaseConnection = Objects.requireNonNull(databaseConnection, 
                "La conexión a la base de datos no puede ser nula");
        this.logger = new CustomLogger();
    }
    
    /**
     * Valida que una transacción cumpla con las reglas de negocio básicas.
     *
     * @param transaction la transacción a validar
     * @throws IllegalArgumentException si la validación falla
     * @throws NullPointerException si la transacción es nula
     */
    protected void validateTransaction(Transaction transaction) {
        Objects.requireNonNull(transaction, "La transacción no puede ser nula");
        
        if (transaction.getAmount() == 0) {
            throw new IllegalArgumentException("El monto de la transacción no puede ser cero");
        }
        
        if (transaction.getDescription() == null || transaction.getDescription().trim().isEmpty()) {
            throw new IllegalArgumentException("La descripción de la transacción no puede estar vacía");
        }
        
        if (transaction.getDate() == null) {
            throw new IllegalArgumentException("La fecha de la transacción no puede ser nula");
        }
    }
    
    /**
     * Valida que un ID sea válido.
     *
     * @param id el ID a validar
     * @throws IllegalArgumentException si el ID no es válido
     */
    protected void validateId(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("El ID debe ser mayor que cero");
        }
    }
    
    // Los métodos de la interfaz se implementarán en las clases hijas
    @Override
    public abstract Transaction createTransaction(Transaction transaction);
    
    @Override
    public abstract Transaction getTransactionById(long id);
    
    @Override
    public abstract Transaction updateTransactionById(long id, Transaction transaction);
    
    @Override
    public abstract void deleteTransactionById(long id);
    
    @Override
    public abstract List<Transaction> getAllTransactions();
}