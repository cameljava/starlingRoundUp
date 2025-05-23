package com.example.starling.roundup.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.starling.roundup.exception.AccountNotFoundException;
import com.example.starling.roundup.exception.InvalidAccountDataException;
import com.example.starling.roundup.model.Account;
import com.example.starling.roundup.model.AccountsResponse;
import com.example.starling.roundup.model.Balance;
import com.example.starling.roundup.model.CurrencyAndAmount;
import com.example.starling.roundup.util.ReactiveServiceTemplate;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Implementation of the AccountService interface. This service provides methods
 * to interact with Starling Bank's API for retrieving account information,
 * default category, and account balance.
 */
@Service
@Slf4j
public class AccountServiceImpl implements AccountService {

    private static final String GET_ACCOUNTS_PATH = "/api/v2/accounts";
    private static final String GET_BALANCE_PATH = "/api/v2/accounts/{accountUid}/balance";

    private final WebClient webClient;

    public AccountServiceImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * {@inheritDoc} Retrieves the first account from the accounts API as the
     * default account.
     *
     * @throws InvalidAccountDataException if the API response is invalid
     * @throws AccountNotFoundException if no accounts are found
     */
    @Override
    public Mono<Account> getDefaultAccount() {
        log.debug("Retrieving default account");

        return ReactiveServiceTemplate.withRetry(
                webClient.get()
                        .uri(GET_ACCOUNTS_PATH)
                        .retrieve()
                        .bodyToMono(AccountsResponse.class),
                "getDefaultAccount"
        ).flatMap(response -> {
            if (response == null) {
                log.error("Received null response from accounts API");
                return Mono.error(new InvalidAccountDataException("Null response from accounts API"));
            }

            List<Account> accounts = response.accounts();
            if (accounts == null || accounts.isEmpty()) {
                log.error("No accounts found for user");
                return Mono.error(new AccountNotFoundException("Account not found"));
            }

            Account defaultAccount = accounts.get(0);
            if (defaultAccount == null) {
                log.error("Default account is null");
                return Mono.error(new InvalidAccountDataException("Default account is null"));
            }

            log.debug("Retrieved default account with ID: {}", defaultAccount.accountUid());
            return Mono.just(defaultAccount);
        });
    }

    /**
     * {@inheritDoc} Gets the default category from the provided account object.
     */
    @Override
    public Mono<UUID> getDefaultCategory(Account account) {
        log.debug("Getting default category for account: {}", account.accountUid());

        return Optional.ofNullable(account.defaultCategory())
                .map(Mono::just)
                .orElseGet(() -> {
                    log.error("Account {} does not have a default category", account.accountUid());
                    return Mono.error(new InvalidAccountDataException("Account does not have default category"));
                });
    }

    /**
     * {@inheritDoc} Fetches the account balance from the balance API endpoint.
     */
    @Override
    public Mono<CurrencyAndAmount> getEffectiveBalance(UUID accountUid) {
        log.debug("Fetching effective balance for account: {}", accountUid);

        return ReactiveServiceTemplate.withRetry(
                webClient.get()
                        .uri(GET_BALANCE_PATH, accountUid)
                        .retrieve()
                        .bodyToMono(Balance.class)
                        .map(Balance::effectiveBalance),
                "getEffectiveBalance"
        );
    }
}
