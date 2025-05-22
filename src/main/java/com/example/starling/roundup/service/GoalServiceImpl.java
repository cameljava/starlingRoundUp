package com.example.starling.roundup.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.starling.roundup.exception.InvalidAccountDataException;
import com.example.starling.roundup.model.CurrencyAndAmount;
import com.example.starling.roundup.model.SavingsGoal;
import com.example.starling.roundup.model.SavingsGoalRequest;
import com.example.starling.roundup.model.SavingsGoalResponse;
import com.example.starling.roundup.model.SavingsGoalTransferResponse;
import com.example.starling.roundup.model.TopUpRequest;

import reactor.core.publisher.Mono;

/**
 * Implementation of the GoalService interface. This service manages savings
 * goals, including: - Getting or creating a round-up savings goal -
 * Transferring funds to the savings goal
 */
@Service
public class GoalServiceImpl implements GoalService {

    private static final Logger log = LoggerFactory.getLogger(GoalServiceImpl.class);

    private static final String GET_SAVINGS_GOALS_PATH = "/api/v2/account/{accountUid}/savings-goals";
    private static final String CREATE_SAVINGS_GOAL_PATH = "/api/v2/account/{accountUid}/savings-goals";
    private static final String TRANSFER_TO_GOAL_PATH = "/api/v2/account/{accountUid}/savings-goals/{savingsGoalUid}/add-money/{transferUid}";

    private final WebClient webClient;
    public final String ROUND_UP_SAVINGS_GOAL_NAME = "Round Up Savings";

    public GoalServiceImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * {@inheritDoc} Finds an existing Round Up Savings goal or creates a new
     * one.
     */
    @Override
    public Mono<SavingsGoal> getOrCreateSavingsGoal(UUID accountUid) {
        log.debug("Getting or creating savings goal for account: {}", accountUid);

        return webClient.get()
                .uri(GET_SAVINGS_GOALS_PATH, accountUid)
                .retrieve()
                .bodyToMono(SavingsGoalResponse.class)
                .flatMap(response -> {
                    if (response == null || response.savingsGoalList() == null) {
                        log.error("Received invalid response for savings goals for account: {}", accountUid);
                        return Mono.error(new InvalidAccountDataException("Get savings goals response invalid"));
                    }

                    List<SavingsGoal> savingsGoalList = response.savingsGoalList();
                    log.debug("Found {} existing savings goals for account {}", savingsGoalList.size(), accountUid);

                    // return existing 'Round Up Savings' goal if present
                    for (SavingsGoal goal : savingsGoalList) {
                        if (ROUND_UP_SAVINGS_GOAL_NAME.equals(goal.name())) {
                            log.debug("Found existing Round Up Savings goal: {}", goal.savingsGoalUid());
                            return Mono.just(goal);
                        }
                    }

                    // none found or list empty, create new savings goal
                    log.info("No Round Up Savings goal found, creating a new one for account {}", accountUid);
                    return createNewSavingsGoal(accountUid);
                });
    }

    /**
     * Creates a new savings goal for the round-up feature.
     *
     * @param accountUid the account to create the savings goal for
     * @return Mono<SavingsGoal> containing the newly created SavingsGoal
     */
    private Mono<SavingsGoal> createNewSavingsGoal(UUID accountUid) {
        log.debug("Creating new Round Up Savings goal for account: {}", accountUid);

        SavingsGoalRequest savingsGoalRequest = new SavingsGoalRequest(
                ROUND_UP_SAVINGS_GOAL_NAME,
                "GBP",
                new CurrencyAndAmount("GBP", 100000L)
        );

        return webClient.post()
                .uri(CREATE_SAVINGS_GOAL_PATH, accountUid)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(savingsGoalRequest)
                .retrieve()
                .bodyToMono(SavingsGoal.class)
                .doOnSuccess(newGoal
                        -> log.info("Created new Round Up Savings goal: {} for account: {}",
                        newGoal != null ? newGoal.savingsGoalUid() : "null", accountUid)
                );
    }

    /**
     * {@inheritDoc} Transfers the specified amount to the savings goal.
     */
    @Override
    public Mono<String> transferToSavingsGoal(UUID accountUid, UUID savingsGoalUid, long amount) {
        log.debug("Transferring {} to savings goal {} for account {}", amount, savingsGoalUid, accountUid);

        String transferUid = UUID.randomUUID().toString();
        TopUpRequest topUpRequest = new TopUpRequest(new CurrencyAndAmount("GBP", amount));

        return webClient.put()
                .uri(TRANSFER_TO_GOAL_PATH, accountUid, savingsGoalUid, transferUid)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(topUpRequest)
                .retrieve()
                .bodyToMono(SavingsGoalTransferResponse.class)
                .flatMap(response -> {
                    if (response == null || response.transferUid() == null) {
                        log.error("Transfer to savings goal failed for account: {}, goal: {}", accountUid, savingsGoalUid);
                        return Mono.error(new InvalidAccountDataException("Transfer money to saving goal response invalid"));
                    }
                    log.info("Successfully transferred {} to savings goal {} for account {}, transfer ID: {}",
                            amount, savingsGoalUid, accountUid, response.transferUid());
                    return Mono.just(response.transferUid());
                });
    }
}
