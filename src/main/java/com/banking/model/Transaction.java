package com.banking.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Transaction {
    private final String id;
    private final Account source;
    private final Account destination;
    private final double amount;
    private final LocalDateTime timestamp;

    public Transaction(String id, Account source, Account destination, double amount, LocalDateTime timestamp) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID cannot be null or empty");
        }
        if (source == null) {
            throw new IllegalArgumentException("Source account cannot be null");
        }
        if (destination == null) {
            throw new IllegalArgumentException("Destination account cannot be null");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive");
        }
        this.id = id;
        this.source = source;
        this.destination = destination;
        this.amount = amount;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }

    public Transaction(Account source, Account destination, double amount) {
        this(UUID.randomUUID().toString(), source, destination, amount, LocalDateTime.now());
    }

    public String getId() {
        return id;
    }

    public Account getSource() {
        return source;
    }

    public Account getDestination() {
        return destination;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Transaction that = (Transaction) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id='" + id + '\'' +
                ", source=" + (source != null ? source.getId() : null) +
                ", destination=" + (destination != null ? destination.getId() : null) +
                ", amount=" + amount +
                ", timestamp=" + timestamp +
                '}';
    }
}
