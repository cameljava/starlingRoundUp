package com.example.starling.roundup.service;

import java.util.UUID;

import com.example.starling.roundup.exception.InvalidAccountDataException;
import com.example.starling.roundup.model.SavingsGoal;

import reactor.core.publisher.Mono;

/**
 * Service interface for managing round-up savings goals.
 * <p>
 * The implementation interacts with the Starling API to retrieve existing
 * savings goals or create new ones, and to transfer funds to a goal.
 * </p>
 */
public interface GoalService {

    /**
     * Retrieves the existing savings goal for the given account by calling the
     * Starling API. If no goal exists, a new savings goal is created.
     *
     * @param accountUid the UUID of the account
     * @return Mono<SavingsGoal> containing the SavingsGoal for the account
     * @throws InvalidAccountDataException if the API response is null or
     * malformed
     */
    Mono<SavingsGoal> getOrCreateSavingsGoal(UUID accountUid);

    /**
     * Transfers the specified amount to a savings goal for the account via the
     * Starling API.
     *
     * @param accountUid the UUID of the account
     * @param savingsGoalUid the UUID of the savings goal
     * @param amount the amount in minor currency units to transfer
     * @return Mono<String> containing the unique transfer UID for tracking
     * @throws InvalidAccountDataException if the API response is null or
     * missing transfer UID
     */
    Mono<String> transferToSavingsGoal(UUID accountUid, UUID savingsGoalUid, long amount);
}
