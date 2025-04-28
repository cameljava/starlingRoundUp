package com.example.starling.roundup.service;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.starling.roundup.exception.InvalidAccountDataException;
import com.example.starling.roundup.model.CurrencyAndAmount;
import com.example.starling.roundup.model.SavingsGoal;
import com.example.starling.roundup.model.SavingsGoalRequest;
import com.example.starling.roundup.model.SavingsGoalResponse;
import com.example.starling.roundup.model.SavingsGoalTransferResponse;
import com.example.starling.roundup.model.TopUpRequest;

/**
 * Implementation of the GoalService interface.
 * This service manages savings goals, including:
 * - Getting or creating a round-up savings goal
 * - Transferring funds to the savings goal
 */
@Service
public class GoalServiceImpl implements GoalService {

    private static final Logger log = LoggerFactory.getLogger(GoalServiceImpl.class);

    private static final String GET_SAVINGS_GOALS_PATH = "/api/v2/account/%s/savings-goals";
    private static final String CREATE_SAVINGS_GOAL_PATH = "/api/v2/account/%s/savings-goals";
    private static final String TRANSFER_TO_GOAL_PATH = "/api/v2/account/%s/savings-goals/%s/add-money/%s";

    private final RestTemplate restTemplate;
    public final String ROUND_UP_SAVINGS_GOAL_NAME = "Round Up Savings";

    public GoalServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * {@inheritDoc} Finds an existing Round Up Savings goal or creates a new
     * one.
     */
    @Override
    public SavingsGoal getOrCreateSavingsGoal(UUID accountUid) {
        log.debug("Getting or creating savings goal for account: {}", accountUid);

        String url = String.format(GET_SAVINGS_GOALS_PATH, accountUid);
        SavingsGoalResponse response = restTemplate.getForObject(url, SavingsGoalResponse.class);

        if (response == null || response.savingsGoalList() == null) {
            log.error("Received invalid response for savings goals for account: {}", accountUid);
            throw new InvalidAccountDataException("Get savings goals response invalid");
        }

        List<SavingsGoal> savingsGoalList = response.savingsGoalList();
        log.debug("Found {} existing savings goals for account {}", savingsGoalList.size(), accountUid);

        // return existing 'Round Up Savings' goal if present
        for (SavingsGoal goal : savingsGoalList) {
            if (ROUND_UP_SAVINGS_GOAL_NAME.equals(goal.name())) {
                log.debug("Found existing Round Up Savings goal: {}", goal.savingsGoalUid());
                return goal;
            }
        }

        // none found or list empty, create new savings goal
        log.info("No Round Up Savings goal found, creating a new one for account {}", accountUid);
        return createNewSavingsGoal(accountUid);
    }

    /**
     * Creates a new savings goal for the round-up feature.
     *
     * @param accountUid the account to create the savings goal for
     * @return the newly created SavingsGoal
     */
    private SavingsGoal createNewSavingsGoal(UUID accountUid) {
        // Assumption, set a random default target for Round Up Savings Goal. 
        // may need make it configurable, and logic to handle the case when target is achieved, or not set.
        log.debug("Creating new Round Up Savings goal for account: {}", accountUid);

        SavingsGoalRequest savingsGoalRequest = new SavingsGoalRequest(
                ROUND_UP_SAVINGS_GOAL_NAME,
                "GBP",
                new CurrencyAndAmount("GBP", 100000L)
        );

        String url = String.format(CREATE_SAVINGS_GOAL_PATH, accountUid);
        SavingsGoal newGoal = restTemplate.postForObject(
                url,
                savingsGoalRequest,
                SavingsGoal.class
        );

        log.info("Created new Round Up Savings goal: {} for account: {}",
                newGoal != null ? newGoal.savingsGoalUid() : "null", accountUid);
        return newGoal;
    }

    /**
     * {@inheritDoc} Transfers the specified amount to the savings goal.
     */
    @Override
    public String transferToSavingsGoal(UUID accountUid, UUID savingsGoalUid, long amount) {
        log.debug("Transferring {} to savings goal {} for account {}", amount, savingsGoalUid, accountUid);

        String transferUid = UUID.randomUUID().toString();
        TopUpRequest topUpRequest = new TopUpRequest(new CurrencyAndAmount("GBP", amount));

        String url = String.format(TRANSFER_TO_GOAL_PATH, accountUid, savingsGoalUid, transferUid);
        ResponseEntity<SavingsGoalTransferResponse> response = restTemplate.exchange(
                url,
                org.springframework.http.HttpMethod.PUT,
                new org.springframework.http.HttpEntity<>(topUpRequest),
                SavingsGoalTransferResponse.class
        );

        SavingsGoalTransferResponse body = response.getBody();
        if (body == null || body.transferUid() == null) {
            log.error("Transfer to savings goal failed for account: {}, goal: {}", accountUid, savingsGoalUid);
            throw new InvalidAccountDataException("Transfer money to saving goal response invalid");
        }

        log.info("Successfully transferred {} to savings goal {} for account {}, transfer ID: {}",
                amount, savingsGoalUid, accountUid, body.transferUid());
        return body.transferUid();
    }
}
