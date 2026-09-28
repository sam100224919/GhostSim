package com.banking.service;

import com.banking.model.Account;
import com.banking.model.Bank;

public class BankService {
    private final Bank bank;

    public BankService() {
        this(new Bank());
    }

    public BankService(Bank bank) {
        if (bank == null) {
            throw new IllegalArgumentException("Bank cannot be null");
        }
        this.bank = bank;
    }

    public Bank getBank() {
        return bank;
    }

    public Account createAccount(String id, double initialBalance) {
        validateId(id, "Account ID");
        if (initialBalance < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative");
        }
        if (bank.hasAccount(id)) {
            throw new IllegalArgumentException("Account already exists with ID: " + id);
        }
        Account account = new Account(id, initialBalance);
        bank.addAccount(account);
        return account;
    }

    public Account createAccount(String id) {
        return createAccount(id, 0.0);
    }

    public Account findAccountById(String id) {
        validateId(id, "Account ID");
        Account account = bank.getAccount(id);
        if (account == null) {
            throw new IllegalArgumentException("Account not found with ID: " + id);
        }
        return account;
    }

    public Account getAccount(String id) {
        return findAccountById(id);
    }

    public void deposit(String id, double amount) {
        validateId(id, "Account ID");
        validateAmount(amount, "Deposit");
        Account account = findAccountById(id);
        account.deposit(amount);
    }

    public void depositMoney(String id, double amount) {
        deposit(id, amount);
    }

    public void withdraw(String id, double amount) {
        validateId(id, "Account ID");
        validateAmount(amount, "Withdrawal");
        Account account = findAccountById(id);
        if (amount > account.getBalance()) {
            throw new IllegalArgumentException("Insufficient funds in account: " + id);
        }
        account.withdraw(amount);
    }

    public void withdrawMoney(String id, double amount) {
        withdraw(id, amount);
    }

    public void transfer(String fromId, String toId, double amount) {
        validateId(fromId, "Source account ID");
        validateId(toId, "Destination account ID");
        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }
        validateAmount(amount, "Transfer");

        Account fromAccount = findAccountById(fromId);
        Account toAccount = findAccountById(toId);

        if (amount > fromAccount.getBalance()) {
            throw new IllegalArgumentException("Insufficient funds in account: " + fromId);
        }

        fromAccount.withdraw(amount);
        toAccount.deposit(amount);
    }

    public void transferMoney(String fromId, String toId, double amount) {
        transfer(fromId, toId, amount);
    }

    private void validateId(String id, String fieldName) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }

    private void validateAmount(double amount, String operation) {
        if (amount <= 0) {
            throw new IllegalArgumentException(operation + " amount must be positive");
        }
    }
}
