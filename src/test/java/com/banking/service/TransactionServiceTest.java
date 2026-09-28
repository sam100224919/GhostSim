package com.banking.service;

import com.banking.model.Account;
import com.banking.model.Transaction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TransactionServiceTest {

    private TransactionService transactionService;
    private Account sourceAccount;
    private Account destinationAccount;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService();
        sourceAccount = new Account("ACC-001", 500.0);
        destinationAccount = new Account("ACC-002", 200.0);
    }

    @Test
    void testSuccessfulTransferAndBalanceChanges() {
        Transaction tx = transactionService.transfer(sourceAccount, destinationAccount, 150.0);

        assertNotNull(tx);
        assertEquals(350.0, sourceAccount.getBalance(), 0.001);
        assertEquals(350.0, destinationAccount.getBalance(), 0.001);
    }

    @Test
    void testTransactionCreation() {
        Transaction tx = transactionService.transfer(sourceAccount, destinationAccount, 100.0);

        assertNotNull(tx);
        assertNotNull(tx.getId());
        assertEquals(sourceAccount, tx.getSource());
        assertEquals(destinationAccount, tx.getDestination());
        assertEquals(100.0, tx.getAmount(), 0.001);
        assertNotNull(tx.getTimestamp());

        assertEquals(1, transactionService.getTransactionHistory().size());
        assertTrue(transactionService.getTransactionHistory().contains(tx));
    }

    @Test
    void testZeroAndNegativeAmountRejection() {
        assertThrows(IllegalArgumentException.class, () ->
                transactionService.transfer(sourceAccount, destinationAccount, 0.0));

        assertThrows(IllegalArgumentException.class, () ->
                transactionService.transfer(sourceAccount, destinationAccount, -50.0));

        // Ensure balances are unchanged
        assertEquals(500.0, sourceAccount.getBalance(), 0.001);
        assertEquals(200.0, destinationAccount.getBalance(), 0.001);
    }

    @Test
    void testInsufficientFundsRejection() {
        assertThrows(IllegalArgumentException.class, () ->
                transactionService.transfer(sourceAccount, destinationAccount, 600.0));

        // Ensure balances are unchanged
        assertEquals(500.0, sourceAccount.getBalance(), 0.001);
        assertEquals(200.0, destinationAccount.getBalance(), 0.001);
    }

    @Test
    void testNullAccountRejection() {
        assertThrows(IllegalArgumentException.class, () ->
                transactionService.transfer(null, destinationAccount, 100.0));

        assertThrows(IllegalArgumentException.class, () ->
                transactionService.transfer(sourceAccount, null, 100.0));

        assertThrows(IllegalArgumentException.class, () ->
                transactionService.transfer(null, null, 100.0));
    }
}
