package com.example.starling.roundup.service;

import java.util.UUID;

import com.example.starling.roundup.exception.AccountNotFoundException;
import com.example.starling.roundup.exception.InvalidAccountDataException;
import com.example.starling.roundup.model.Account;
import com.example.starling.roundup.model.CurrencyAndAmount;

import reactor.core.publisher.Mono;

/**
 * Service interface for managing user accounts. Provides methods to retrieve
 * the default account, default category ID, and effective balance. Assumptions:
 * - If starling api get accounts return empty list, throw
 * AccountNotFoundException, assume user has not connected with an account,
 * return 404 not found error - If starling api get accounts return account
 * list, use first account as default account - Account default category is
 * always available otherwise throw InvalidAccountDataException, this will be
 * treated as bad data, return 500 internal server error - If starling api get
 * effective return null, throw InvalidAccountDataException, this will be
 * treated as bad data, return 500 internal server error
 */
public interface AccountService {

    /**
     * Retrieves the default account for the current user.
     *
     * @return Mono<Account> containing the default Account
     * @throws InvalidAccountDataException if account data is invalid
     * @throws AccountNotFoundException if no account is found
     */
    Mono<Account> getDefaultAccount();

    /**
     * Retrieves the default savings category ID for the specified account.
     *
     * @param account the account to retrieve the default category from
     * @return Mono<UUID> containing the UUID of the default category
     * @throws InvalidAccountDataException if the default category is missing or
     * invalid
     */
    Mono<UUID> getDefaultCategory(Account account);

    /**
     * Retrieves the effective balance for the specified account.
     *
     * @param accountUid the unique identifier of the account
     * @return Mono<CurrencyAndAmount> containing the effective balance
     * @throws InvalidAccountDataException if unable to fetch the balance
     */
    Mono<CurrencyAndAmount> getEffectiveBalance(UUID accountUid);
}
