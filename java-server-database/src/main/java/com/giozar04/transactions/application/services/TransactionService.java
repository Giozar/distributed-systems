 package com.giozar04.transactions.application.services;

import java.util.List;

import com.giozar04.transactions.domain.entities.Transaction;
import com.giozar04.transactions.domain.interfaces.TransactionRepository;

public class TransactionService {

    private final TransactionRepository transactionRepository;

    public TransactionService (TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    public Transaction createTransaction(Transaction transaction) {
        return transactionRepository.createTransaction(transaction);
    } 

    public Transaction getTransactionById(long id) {
        return transactionRepository.getTransactionById(id);
    }
    public Transaction updateTransactionById(long id, Transaction transaction) {
        return transactionRepository.updateTransactionById(id, transaction);
    }
    public void deleteTransactionById(long id) {
        transactionRepository.deleteTransactionById(id);
    }
    public List<Transaction> getAllTransactions() {
        return transactionRepository.getAllTransactions();
    }

}
