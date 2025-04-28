package com.example.starling.roundup.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.starling.roundup.exception.InsufficientBalanceException;
import com.example.starling.roundup.model.Account;
import com.example.starling.roundup.model.CurrencyAndAmount;
import com.example.starling.roundup.model.FeedItem;
import com.example.starling.roundup.model.SavingsGoal;

/**
 * Implementation of the RoundUpService interface.
 * This service handles the core business logic of the round-up feature:
 * 1. Retrieving the user's transactions for the past week
 * 2. Calculating the round-up amount for these transactions
 * 3. Transferring the round-up amount to the user's savings goal
 */
@Service
public class RoundUpServiceImpl implements RoundUpService {

    private static final Logger log = LoggerFactory.getLogger(RoundUpServiceImpl.class);

    private final TransactionFeedItemService transactionFeedItemService;
    private final GoalService goalService;
    private final AccountService accountService;

    public RoundUpServiceImpl(
            TransactionFeedItemService transactionFeedItemService,
            GoalService goalService,
            AccountService accountService) {
        this.transactionFeedItemService = transactionFeedItemService;
        this.goalService = goalService;
        this.accountService = accountService;
    }

    /**
     * {@inheritDoc}
     * Processes the round-up flow:
     * 1. Gets the default account and category
     * 2. Gets or creates a savings goal
     * 3. Retrieves transactions for the past week
     * 4. Calculates the total round-up amount
     * 5. If sufficient balance exists, transfers the amount to the savings goal
     * 
     * @throws InsufficientBalanceException if the account balance is too low
     * for the transfer
     */
    @Override
    public void roundUpTransactions() {
        log.info("Starting round-up transaction process");

        // Get default account
        Account defaultAccount = accountService.getDefaultAccount();
        UUID accountUid = defaultAccount.accountUid();
        log.debug("Using default account: {}", accountUid);

        // Get default category
        UUID defaultCategoryUid = accountService.getDefaultCategory(defaultAccount);
        log.debug("Using default category: {}", defaultCategoryUid);

        // Get or create savings goal
        SavingsGoal savingsGoal = goalService.getOrCreateSavingsGoal(accountUid);
        log.debug("Using savings goal: {}", savingsGoal.savingsGoalUid());

        // Get transactions for the last week and calculate round up amount
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minus(7, ChronoUnit.DAYS);
        log.debug("Fetching transactions from {} to {}", weekAgo, now);

        List<FeedItem> feedItems = transactionFeedItemService.getFeedItemsForDateRange(
                accountUid,
                defaultCategoryUid,
                weekAgo,
                now
        );

        log.debug("Found {} transactions for processing", feedItems.size());

        long totalRoundUp = transactionFeedItemService.calculateRoundUpAmount(feedItems);
        log.info("Calculated total round-up amount: {}", totalRoundUp);

        if (totalRoundUp > 0) {
            CurrencyAndAmount balance = accountService.getEffectiveBalance(accountUid);
            log.debug("Current account balance: {}", balance.minorUnits());

            if (balance.minorUnits() >= totalRoundUp) {
                log.info("Transferring {} to savings goal {}", totalRoundUp, savingsGoal.savingsGoalUid());
                goalService.transferToSavingsGoal(
                        accountUid,
                        UUID.fromString(savingsGoal.savingsGoalUid()),
                        totalRoundUp
                );
                log.info("Round-up process completed successfully");
            } else {
                log.warn("Insufficient balance ({}) to transfer round-up amount ({})",
                        balance.minorUnits(), totalRoundUp);
                throw new InsufficientBalanceException("Insufficient balance to round up");
            }
        } else {
            log.info("No round-up amount to transfer");
        }
    }
}
