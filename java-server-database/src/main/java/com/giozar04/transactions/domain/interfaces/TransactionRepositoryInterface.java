package com.giozar04.transactions.domain.interfaces;

import java.util.List;

import com.giozar04.transactions.domain.entities.Transaction;

public interface TransactionRepositoryInterface {
    Transaction createTransaction( Transaction transaction);
    Transaction getTransactionById(long id);
    Transaction updateTransactionById(long id, Transaction transaction);
    void deleteTransactionById(long id);
    List<Transaction> getAllTransactions();

}
