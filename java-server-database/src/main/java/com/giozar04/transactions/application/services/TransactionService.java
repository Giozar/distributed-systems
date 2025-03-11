 package com.giozar04.transactions.application.services;

import java.util.List;

import com.giozar04.transactions.domain.entities.Transaction;
import com.giozar04.transactions.domain.interfaces.TransactionRepositoryInterface;

public class TransactionService implements  TransactionRepositoryInterface {

    private final TransactionRepositoryInterface transactionRepository;

    public TransactionService (TransactionRepositoryInterface transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public Transaction createTransaction(Transaction transaction) {
        return transactionRepository.createTransaction(transaction);
    } 

    @Override
    public Transaction getTransactionById(long id) {
        return transactionRepository.getTransactionById(id);
    }
    @Override
    public Transaction updateTransactionById(long id, Transaction transaction) {
        return transactionRepository.updateTransactionById(id, transaction);
    }
    @Override
    public void deleteTransactionById(long id) {
        transactionRepository.deleteTransactionById(id);
    }
    @Override
    public List<Transaction> getAllTransactions() {
        return transactionRepository.getAllTransactions();
    }

}
