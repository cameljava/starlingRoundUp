package com.example.starling.roundup.service;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import com.example.starling.roundup.exception.AccountNotFoundException;
import com.example.starling.roundup.exception.InvalidAccountDataException;
import com.example.starling.roundup.model.Account;
import com.example.starling.roundup.model.AccountsResponse;
import com.example.starling.roundup.model.Balance;
import com.example.starling.roundup.model.CurrencyAndAmount;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    private AccountService accountService;

    private final UUID accountUid = UUID.randomUUID();
    private final UUID defaultCategoryId = UUID.randomUUID();

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        accountService = new AccountServiceImpl(restTemplate);
    }

    @Test
    void getDefaultAccount_Success() {
        // Given
        Account account = new Account(accountUid, defaultCategoryId, "PRIMARY", "GBP");
        AccountsResponse accountsResponse = new AccountsResponse(List.of(account));
        when(restTemplate.getForObject(eq("/api/v2/accounts"), eq(AccountsResponse.class)))
                .thenReturn(accountsResponse);

        // When
        Account result = accountService.getDefaultAccount();

        // Then
        assertNotNull(result);
        assertEquals(accountUid, result.accountUid());
        assertEquals(defaultCategoryId, result.defaultCategory());
        assertEquals("PRIMARY", result.accountType());
        assertEquals("GBP", result.currency());
    }

    @Test
    void getDefaultAccount_ThrowsException_WhenNoAccountsFound() {
        // Given
        AccountsResponse emptyResponse = new AccountsResponse(Collections.emptyList());
        when(restTemplate.getForObject(eq("/api/v2/accounts"), eq(AccountsResponse.class)))
                .thenReturn(emptyResponse);

        // When/Then
        AccountNotFoundException exception = assertThrows(AccountNotFoundException.class,
                () -> accountService.getDefaultAccount());
        assertEquals("Account not found", exception.getMessage());
    }

    @Test
    void getDefaultAccount_ThrowsException_WhenResponseIsNull() {
        // Given
        when(restTemplate.getForObject(eq("/api/v2/accounts"), eq(AccountsResponse.class)))
                .thenReturn(null);

        // When/Then
        InvalidAccountDataException exception = assertThrows(InvalidAccountDataException.class,
                () -> accountService.getDefaultAccount());
        assertEquals("Account data not valid", exception.getMessage());
    }

    @Test
    void getDefaultCategory_Success() {
        // Given
        Account account = new Account(accountUid, defaultCategoryId, "PRIMARY", "GBP");

        // When
        UUID result = accountService.getDefaultCategory(account);

        // Then
        assertEquals(defaultCategoryId, result);
    }

    @Test
    void getDefaultCategory_ThrowsException_WhenAccountHasNoDefaultCategory() {
        // Given
        Account accountWithoutCategory = new Account(accountUid, null, "PRIMARY", "GBP");

        // When/Then
        InvalidAccountDataException exception = assertThrows(InvalidAccountDataException.class,
                () -> accountService.getDefaultCategory(accountWithoutCategory));
        assertEquals("Account does not have default category", exception.getMessage());
    }

    @Test
    void getEffectiveBalance_Success() {
        // Given
        CurrencyAndAmount currencyAndAmount = new CurrencyAndAmount("GBP", 1234L);
        Balance balance = new Balance(currencyAndAmount, currencyAndAmount, currencyAndAmount, currencyAndAmount, currencyAndAmount);
        when(restTemplate.getForObject(eq("/api/v2/accounts/{accountUid}/balance"), eq(Balance.class), eq(accountUid)))
                .thenReturn(balance);

        // When
        CurrencyAndAmount result = accountService.getEffectiveBalance(accountUid);

        // Then
        assertEquals(currencyAndAmount, result);
    }

    @Test
    void getEffectiveBalance_ThrowsException_WhenBalanceResponseIsNull() {
        // Given
        when(restTemplate.getForObject(eq("/api/v2/accounts/{accountUid}/balance"), eq(Balance.class), eq(accountUid)))
                .thenReturn(null);

        // When/Then
        InvalidAccountDataException exception = assertThrows(InvalidAccountDataException.class,
                () -> accountService.getEffectiveBalance(accountUid));
        assertEquals("Failed to fetch account balance", exception.getMessage());
    }
}
