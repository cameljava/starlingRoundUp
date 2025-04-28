package com.example.starling.roundup.service;

import java.util.UUID;

import com.example.starling.roundup.model.Account;
import com.example.starling.roundup.model.CurrencyAndAmount;

/**
 * Service interface for managing user accounts.
 * Provides methods to retrieve the default account,
 * default category ID, and effective balance.
 * Assumptions:
 * - If starling api get accounts return empty list, throw AccountNotFoundException, assume user has not connected with an account, 
 *   return 404 not found error
 * - If starling api get accounts return account list, use first account as default account
 * - Account default category is always available otherwise throw InvalidAccountDataException, this will be treated as bad data, return 500 internal server error
 * - If starling api get effective return null, throw InvalidAccountDataException, this will be treated as bad data, return 500 internal server error
 */
public interface AccountService {

    /**
     * Retrieves the default account for the current user.
     *
     * @return the default Account
     * @throws InvalidAccountDataException if account data is invalid
     * @throws AccountNotFoundException if no account is found
     */
    Account getDefaultAccount();

    /**
     * Retrieves the default savings category ID for the specified account.
     *
     * @param account the account to retrieve the default category from
     * @return the UUID of the default category
     * @throws InvalidAccountDataException if the default category is missing or
     * invalid
     */
    UUID getDefaultCategory(Account account);

    /**
     * Retrieves the effective balance for the specified account.
     *
     * @param accountUid the unique identifier of the account
     * @return the effective balance as a CurrencyAndAmount
     * @throws InvalidAccountDataException if unable to fetch the balance
     */
    CurrencyAndAmount getEffectiveBalance(UUID accountUid);
}
