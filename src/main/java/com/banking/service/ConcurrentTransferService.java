package com.banking.service;

import com.banking.model.Account;

public class ConcurrentTransferService {

    public void transfer(Account source, Account destination, double amount) {
        if (source == null) {
            throw new IllegalArgumentException("Source account cannot be null");
        }
        if (destination == null) {
            throw new IllegalArgumentException("Destination account cannot be null");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        if (source.equals(destination)) {
            throw new IllegalArgumentException("Source and destination accounts must be different");
        }

        Account firstLock;
        Account secondLock;

        int comparison = source.getId().compareTo(destination.getId());
        if (comparison < 0) {
            firstLock = source;
            secondLock = destination;
        } else if (comparison > 0) {
            firstLock = destination;
            secondLock = source;
        } else {
            int hash1 = System.identityHashCode(source);
            int hash2 = System.identityHashCode(destination);
            if (hash1 < hash2) {
                firstLock = source;
                secondLock = destination;
            } else if (hash1 > hash2) {
                firstLock = destination;
                secondLock = source;
            } else {
                synchronized (ConcurrentTransferService.class) {
                    synchronized (source) {
                        synchronized (destination) {
                            performTransfer(source, destination, amount);
                            return;
                        }
                    }
                }
            }
        }

        synchronized (firstLock) {
            synchronized (secondLock) {
                performTransfer(source, destination, amount);
            }
        }
    }

    private void performTransfer(Account source, Account destination, double amount) {
        if (source.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient funds in source account");
        }
        source.withdraw(amount);
        destination.deposit(amount);
    }
}
