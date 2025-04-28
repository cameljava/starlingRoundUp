package com.example.starling.roundup.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.starling.roundup.exception.AccountNotFoundException;
import com.example.starling.roundup.exception.InvalidAccountDataException;
import com.example.starling.roundup.model.Account;
import com.example.starling.roundup.model.AccountsResponse;
import com.example.starling.roundup.model.Balance;
import com.example.starling.roundup.model.CurrencyAndAmount;

/**
 * Implementation of the AccountService interface. This service provides methods
 * to interact with Starling Bank's API for retrieving account information,
 * default category, and account balance.
 */
@Service
public class AccountServiceImpl implements AccountService {

    private static final Logger log = LoggerFactory.getLogger(AccountServiceImpl.class);

    private static final String GET_ACCOUNTS_PATH = "/api/v2/accounts";
    private static final String GET_BALANCE_PATH = "/api/v2/accounts/{accountUid}/balance";

    private final RestTemplate restTemplate;

    public AccountServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * {@inheritDoc} Retrieves the first account from the accounts API as the
     * default account.
     *
     * @throws InvalidAccountDataException if the API response is invalid
     * @throws AccountNotFoundException if no accounts are found
     */
    @Override
    public Account getDefaultAccount() {
        log.debug("Retrieving default account");

        List<Account> accounts = Optional.ofNullable(restTemplate.getForObject(GET_ACCOUNTS_PATH, AccountsResponse.class))
                .map(AccountsResponse::accounts)
                .orElseThrow(() -> {
                    log.error("Failed to retrieve valid account data from API");
                    return new InvalidAccountDataException("Account data not valid");
                });

        if (accounts.isEmpty()) {
            log.error("No accounts found for user");
            throw new AccountNotFoundException("Account not found");
        }

        log.debug("Retrieved default account with ID: {}", accounts.get(0).accountUid());
        return accounts.get(0);
    }

    /**
     * {@inheritDoc} Gets the default category from the provided account object.
     */
    @Override
    public UUID getDefaultCategory(Account account) {
        log.debug("Getting default category for account: {}", account.accountUid());

        return Optional.ofNullable(account.defaultCategory())
                .orElseThrow(() -> {
                    log.error("Account {} does not have a default category", account.accountUid());
                    return new InvalidAccountDataException("Account does not have default category");
                });
    }

    /**
     * {@inheritDoc} Fetches the account balance from the balance API endpoint.
     */
    @Override
    public CurrencyAndAmount getEffectiveBalance(UUID accountUid) {
        log.debug("Fetching effective balance for account: {}", accountUid);

        Balance balance = Optional.ofNullable(
                restTemplate.getForObject(
                        GET_BALANCE_PATH,
                        Balance.class,
                        accountUid
                )
        )
                .orElseThrow(() -> {
                    log.error("Failed to fetch balance for account: {}", accountUid);
                    return new InvalidAccountDataException("Failed to fetch account balance");
                });

        log.debug("Retrieved balance for account {}: {}", accountUid, balance.effectiveBalance().minorUnits());
        return balance.effectiveBalance();
    }
}
