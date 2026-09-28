package com.banking.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Bank {
    private final String name;
    private final Map<String, Account> accounts = new HashMap<>();

    public Bank() {
        this("Default Bank");
    }

    public Bank(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Bank name cannot be null or empty");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void addAccount(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        accounts.put(account.getId(), account);
    }

    public Account getAccount(String id) {
        return accounts.get(id);
    }

    public boolean hasAccount(String id) {
        return accounts.containsKey(id);
    }

    public Map<String, Account> getAccounts() {
        return Collections.unmodifiableMap(accounts);
    }
}
