package com.banking.service;

import com.banking.model.Account;
import com.banking.model.Transaction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TransactionService {

    private final List<Transaction> transactionHistory = new ArrayList<>();

    public Transaction transfer(Account source, Account destination, double amount) {
        if (source == null) {
            throw new IllegalArgumentException("Source account cannot be null");
        }
        if (destination == null) {
            throw new IllegalArgumentException("Destination account cannot be null");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }
        if (source.equals(destination)) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }
        if (source.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient funds in source account");
        }

        source.withdraw(amount);
        destination.deposit(amount);

        Transaction transaction = new Transaction(source, destination, amount);
        transactionHistory.add(transaction);
        return transaction;
    }

    public List<Transaction> getTransactionHistory() {
        return Collections.unmodifiableList(transactionHistory);
    }
}
