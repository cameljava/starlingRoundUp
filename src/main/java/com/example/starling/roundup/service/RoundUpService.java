package com.example.starling.roundup.service;

import reactor.core.publisher.Mono;

/**
 * Service interface for orchestrating automated round-up operations on user
 * transactions.
 * <p>
 * This operation includes:
 * <ul>
 * <li>Retrieving the default account and savings category</li>
 * <li>Fetching recent transactions and calculating the total round-up
 * amount</li>
 * <li>Transferring the calculated round-up funds to the savings goal</li>
 * </ul>
 * </p>
 */
public interface RoundUpService {

    /**
     * Executes the round-up process for the default account.
     * <p>
     * Orchestrates fetching account details, calculating round-up from recent
     * transactions, and performing the transfer to the savings goal.
     * </p>
     *
     * @return Mono<Void> that completes when the round-up process is finished
     * @throws
     * com.example.starling.roundup.exception.InvalidAccountDataException if
     * account or category data is invalid
     * @throws
     * com.example.starling.roundup.exception.InsufficientBalanceException if
     * balance is insufficient for the transfer
     */
    Mono<Void> roundUpTransactions();
}
