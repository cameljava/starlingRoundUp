package com.example.starling.roundup.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersUriSpec;
import org.springframework.web.reactive.function.client.WebClient.ResponseSpec;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.example.starling.roundup.exception.AccountNotFoundException;
import com.example.starling.roundup.exception.InvalidAccountDataException;
import com.example.starling.roundup.model.Account;
import com.example.starling.roundup.model.AccountsResponse;
import com.example.starling.roundup.model.Balance;
import com.example.starling.roundup.model.CurrencyAndAmount;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AccountServiceImplTest {

    @Mock
    private WebClient webClient;

    @Mock
    private RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private ResponseSpec responseSpec;

    private AccountServiceImpl accountService;

    private static final UUID TEST_ACCOUNT_UID = UUID.randomUUID();
    private static final UUID TEST_CATEGORY_UID = UUID.randomUUID();
    private static final String TEST_CURRENCY = "GBP";
    private static final Long TEST_AMOUNT = 1000L;

    @BeforeEach
    void setUp() {
        accountService = new AccountServiceImpl(webClient);
    }

    @Test
    void getDefaultAccount_Success() {
        // Given
        setupWebClientMocks();
        Account testAccount = new Account(TEST_ACCOUNT_UID, TEST_CATEGORY_UID, "PRIMARY", TEST_CURRENCY);
        AccountsResponse accountsResponse = new AccountsResponse(List.of(testAccount));
        when(responseSpec.bodyToMono(AccountsResponse.class))
                .thenReturn(Mono.just(accountsResponse));

        // When
        Mono<Account> result = accountService.getDefaultAccount();

        // Then
        StepVerifier.create(result)
                .expectNext(testAccount)
                .verifyComplete();
    }

    @Test
    void getDefaultAccount_RetryOn5xxError() {
        // Given
        setupWebClientMocks();
        Account testAccount = new Account(TEST_ACCOUNT_UID, TEST_CATEGORY_UID, "PRIMARY", TEST_CURRENCY);
        AccountsResponse accountsResponse = new AccountsResponse(List.of(testAccount));
        
        AtomicInteger attemptCounter = new AtomicInteger(0);
        when(responseSpec.bodyToMono(AccountsResponse.class))
            .thenReturn(Mono.fromCallable(() -> {
                if (attemptCounter.getAndIncrement() == 0) {
                    throw WebClientResponseException.create(500, "Server Error", null, null, null);
                }
                return accountsResponse;
            }));

        // When
        Mono<Account> result = accountService.getDefaultAccount();

        // Then
        StepVerifier.create(result)
                .expectNext(testAccount)
                .verifyComplete();
    }

    @Test
    void getDefaultAccount_NoRetryOn4xxError() {
        // Given
        setupWebClientMocks();
        when(responseSpec.bodyToMono(AccountsResponse.class))
                .thenReturn(Mono.error(WebClientResponseException.create(404, "Not Found", null, null, null)));

        // When
        Mono<Account> result = accountService.getDefaultAccount();

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(error -> 
                    error instanceof InvalidAccountDataException &&
                    error.getMessage().contains("Failed to execute getDefaultAccount") &&
                    error.getMessage().contains("404 Not Found"))
                .verify();
    }

    @Test
    void getDefaultAccount_NoAccounts_ThrowsAccountNotFoundException() {
        // Given
        setupWebClientMocks();
        AccountsResponse emptyResponse = new AccountsResponse(List.of());
        when(responseSpec.bodyToMono(AccountsResponse.class))
                .thenReturn(Mono.just(emptyResponse));

        // When
        Mono<Account> result = accountService.getDefaultAccount();

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(error -> 
                    error instanceof AccountNotFoundException &&
                    error.getMessage().contains("Account not found"))
                .verify();
    }

    @Test
    void getDefaultCategory_Success() {
        // Given
        Account account = new Account(TEST_ACCOUNT_UID, TEST_CATEGORY_UID, "PRIMARY", TEST_CURRENCY);

        // When
        Mono<UUID> result = accountService.getDefaultCategory(account);

        // Then
        StepVerifier.create(result)
                .expectNext(TEST_CATEGORY_UID)
                .verifyComplete();
    }

    @Test
    void getDefaultCategory_NullCategory_ThrowsInvalidAccountDataException() {
        // Given
        Account account = new Account(TEST_ACCOUNT_UID, null, "PRIMARY", TEST_CURRENCY);

        // When
        Mono<UUID> result = accountService.getDefaultCategory(account);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(error -> 
                    error instanceof InvalidAccountDataException &&
                    error.getMessage().contains("Account does not have default category"))
                .verify();
    }

    @Test
    void getEffectiveBalance_Success() {
        // Given
        setupWebClientMocks();
        CurrencyAndAmount expectedBalance = new CurrencyAndAmount(TEST_CURRENCY, TEST_AMOUNT);
        Balance balance = new Balance(expectedBalance, expectedBalance, expectedBalance, expectedBalance, expectedBalance);
        when(responseSpec.bodyToMono(Balance.class))
                .thenReturn(Mono.just(balance));

        // When
        Mono<CurrencyAndAmount> result = accountService.getEffectiveBalance(TEST_ACCOUNT_UID);

        // Then
        StepVerifier.create(result)
                .expectNext(expectedBalance)
                .verifyComplete();
    }

    @Test
    void getEffectiveBalance_RetryOn5xxError() {
        // Given
        setupWebClientMocks();
        CurrencyAndAmount expectedBalance = new CurrencyAndAmount(TEST_CURRENCY, TEST_AMOUNT);
        Balance balance = new Balance(expectedBalance, expectedBalance, expectedBalance, expectedBalance, expectedBalance);

        AtomicInteger attemptCounter = new AtomicInteger(0);
        when(responseSpec.bodyToMono(Balance.class))
            .thenReturn(Mono.fromCallable(() -> {
                if (attemptCounter.getAndIncrement() == 0) {
                    throw WebClientResponseException.create(500, "Server Error", null, null, null);
                }
                return balance;
            }));

        // When
        Mono<CurrencyAndAmount> result = accountService.getEffectiveBalance(TEST_ACCOUNT_UID);

        // Then
        StepVerifier.create(result)
                .expectNext(expectedBalance)
                .verifyComplete();
    }

    @Test
    void getEffectiveBalance_NoRetryOn4xxError() {
        // Given
        setupWebClientMocks();
        when(responseSpec.bodyToMono(Balance.class))
                .thenReturn(Mono.error(WebClientResponseException.create(404, "Not Found", null, null, null)));

        // When
        Mono<CurrencyAndAmount> result = accountService.getEffectiveBalance(TEST_ACCOUNT_UID);

        // Then
        StepVerifier.create(result)
                .expectErrorMatches(error -> 
                    error instanceof InvalidAccountDataException &&
                    error.getMessage().contains("Failed to execute getEffectiveBalance") &&
                    error.getMessage().contains("404 Not Found"))
                .verify();
    }

    private void setupWebClientMocks() {
        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersUriSpec).when(requestHeadersUriSpec).uri(any(String.class), any(Object[].class));
        doReturn(responseSpec).when(requestHeadersUriSpec).retrieve();
    }
}
