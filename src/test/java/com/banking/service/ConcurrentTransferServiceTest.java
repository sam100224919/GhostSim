package com.banking.service;

import com.banking.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ConcurrentTransferServiceTest {

    private ConcurrentTransferService transferService;
    private Account accountA;
    private Account accountB;

    @BeforeEach
    void setUp() {
        transferService = new ConcurrentTransferService();
        accountA = new Account("ACC-A", 1000.0);
        accountB = new Account("ACC-B", 1000.0);
    }

    @Test
    void testNormalTransferWorks() {
        transferService.transfer(accountA, accountB, 250.0);

        assertEquals(750.0, accountA.getBalance(), 0.001);
        assertEquals(1250.0, accountB.getBalance(), 0.001);
    }

    @Test
    void testInvalidAmountsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                transferService.transfer(accountA, accountB, 0.0));

        assertThrows(IllegalArgumentException.class, () ->
                transferService.transfer(accountA, accountB, -100.0));

        // Balances remain untouched
        assertEquals(1000.0, accountA.getBalance(), 0.001);
        assertEquals(1000.0, accountB.getBalance(), 0.001);
    }

    @Test
    void testInsufficientFundsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                transferService.transfer(accountA, accountB, 1500.0));

        // Balances remain untouched
        assertEquals(1000.0, accountA.getBalance(), 0.001);
        assertEquals(1000.0, accountB.getBalance(), 0.001);
    }

    @Test
    void testNullAndSameAccountRejected() {
        assertThrows(IllegalArgumentException.class, () ->
                transferService.transfer(null, accountB, 100.0));

        assertThrows(IllegalArgumentException.class, () ->
                transferService.transfer(accountA, null, 100.0));

        assertThrows(IllegalArgumentException.class, () ->
                transferService.transfer(null, null, 100.0));

        assertThrows(IllegalArgumentException.class, () ->
                transferService.transfer(accountA, accountA, 100.0));
    }

    @Test
    void testConcurrentBidirectionalTransfersPreserveBalancesAndPreventDeadlock() throws InterruptedException {
        int threadsCount = 50;
        int transfersPerThread = 100;
        double transferAmount = 5.0;

        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadsCount);

        for (int i = 0; i < threadsCount; i++) {
            final boolean forwardFirst = (i % 2 == 0);
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < transfersPerThread; j++) {
                        if (forwardFirst) {
                            transferService.transfer(accountA, accountB, transferAmount);
                            transferService.transfer(accountB, accountA, transferAmount);
                        } else {
                            transferService.transfer(accountB, accountA, transferAmount);
                            transferService.transfer(accountA, accountB, transferAmount);
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(10, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Transfers timed out - potential deadlock");
        assertEquals(1000.0, accountA.getBalance(), 0.001);
        assertEquals(1000.0, accountB.getBalance(), 0.001);
        assertEquals(2000.0, accountA.getBalance() + accountB.getBalance(), 0.001);
    }

    @Test
    void testMultipleConcurrentTransfersTotalMoneyConservation() throws InterruptedException {
        int accountCount = 10;
        double initialBalance = 1000.0;
        double totalInitialMoney = accountCount * initialBalance;

        List<Account> accounts = new ArrayList<>();
        for (int i = 0; i < accountCount; i++) {
            accounts.add(new Account("ACC-" + i, initialBalance));
        }

        int threadsCount = 20;
        int transfersPerThread = 200;
        ExecutorService executor = Executors.newFixedThreadPool(threadsCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadsCount);

        for (int i = 0; i < threadsCount; i++) {
            executor.submit(() -> {
                Random random = new Random();
                try {
                    startLatch.await();
                    for (int j = 0; j < transfersPerThread; j++) {
                        int srcIndex = random.nextInt(accountCount);
                        int dstIndex = random.nextInt(accountCount);
                        if (srcIndex != dstIndex) {
                            Account src = accounts.get(srcIndex);
                            Account dst = accounts.get(dstIndex);
                            double amount = 1.0 + random.nextInt(20);
                            try {
                                transferService.transfer(src, dst, amount);
                            } catch (IllegalArgumentException ignored) {
                                // Ignore if insufficient funds
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean completed = finishLatch.await(15, TimeUnit.SECONDS);
        executor.shutdown();

        assertTrue(completed, "Transfers timed out - potential deadlock");

        double totalCurrentMoney = 0.0;
        for (Account account : accounts) {
            assertTrue(account.getBalance() >= 0.0, "Account balance should not be negative");
            totalCurrentMoney += account.getBalance();
        }

        assertEquals(totalInitialMoney, totalCurrentMoney, 0.001, "Total money must be conserved");
    }
}
