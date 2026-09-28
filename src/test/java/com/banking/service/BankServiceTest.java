package com.banking.service;

import com.banking.model.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BankServiceTest {

    private BankService bankService;

    @BeforeEach
    void setUp() {
        bankService = new BankService();
    }

    @Test
    void testCreateAndFindAccount() {
        Account created = bankService.createAccount("ACC-001", 100.0);
        assertNotNull(created);
        assertEquals("ACC-001", created.getId());
        assertEquals(100.0, created.getBalance(), 0.001);

        Account found = bankService.findAccountById("ACC-001");
        assertNotNull(found);
        assertEquals("ACC-001", found.getId());
        assertEquals(100.0, found.getBalance(), 0.001);
    }

    @Test
    void testDepositAndWithdraw() {
        bankService.createAccount("ACC-002", 50.0);

        bankService.deposit("ACC-002", 150.0);
        Account account = bankService.findAccountById("ACC-002");
        assertEquals(200.0, account.getBalance(), 0.001);

        bankService.withdraw("ACC-002", 80.0);
        assertEquals(120.0, account.getBalance(), 0.001);
    }

    @Test
    void testTransferMoney() {
        bankService.createAccount("ACC-SRC", 300.0);
        bankService.createAccount("ACC-DST", 100.0);

        bankService.transfer("ACC-SRC", "ACC-DST", 150.0);

        assertEquals(150.0, bankService.findAccountById("ACC-SRC").getBalance(), 0.001);
        assertEquals(250.0, bankService.findAccountById("ACC-DST").getBalance(), 0.001);
    }

    @Test
    void testValidationNullAndInvalidAmounts() {
        assertThrows(IllegalArgumentException.class, () -> bankService.createAccount(null, 100.0));
        assertThrows(IllegalArgumentException.class, () -> bankService.createAccount("   ", 100.0));
        assertThrows(IllegalArgumentException.class, () -> bankService.createAccount("ACC-INV", -50.0));

        bankService.createAccount("ACC-VALID", 100.0);
        assertThrows(IllegalArgumentException.class, () -> bankService.deposit("ACC-VALID", -10.0));
        assertThrows(IllegalArgumentException.class, () -> bankService.deposit("ACC-VALID", 0.0));
        assertThrows(IllegalArgumentException.class, () -> bankService.withdraw("ACC-VALID", -5.0));
    }

    @Test
    void testValidationMissingAccountsAndInsufficientFunds() {
        assertThrows(IllegalArgumentException.class, () -> bankService.findAccountById("NON-EXISTENT"));

        bankService.createAccount("ACC-FUNDS", 50.0);
        assertThrows(IllegalArgumentException.class, () -> bankService.withdraw("ACC-FUNDS", 100.0));

        bankService.createAccount("ACC-DST2", 50.0);
        assertThrows(IllegalArgumentException.class, () -> bankService.transfer("ACC-FUNDS", "NON-EXISTENT", 20.0));
        assertThrows(IllegalArgumentException.class, () -> bankService.transfer("ACC-FUNDS", "ACC-DST2", 200.0));
    }
}
