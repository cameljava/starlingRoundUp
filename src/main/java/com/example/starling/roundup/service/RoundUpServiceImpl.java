package com.example.starling.roundup.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.example.starling.roundup.exception.InsufficientBalanceException;

import reactor.core.publisher.Mono;

/**
 * Implementation of the RoundUpService interface. This service handles the core
 * business logic of the round-up feature: 1. Retrieving the user's transactions
 * for the past week 2. Calculating the round-up amount for these transactions
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
     * {@inheritDoc} Processes the round-up flow: 1. Gets the default account
     * and category 2. Gets or creates a savings goal 3. Retrieves transactions
     * for the past week 4. Calculates the total round-up amount 5. If
     * sufficient balance exists, transfers the amount to the savings goal
     *
     * @throws InsufficientBalanceException if the account balance is too low
     * for the transfer
     */
    @Override
    public Mono<Void> roundUpTransactions() {
        log.info("Starting round-up transaction process");

        return accountService.getDefaultAccount()
                .flatMap(defaultAccount -> {
                    UUID accountUid = defaultAccount.accountUid();
                    log.debug("Using default account: {}", accountUid);

                    return accountService.getDefaultCategory(defaultAccount)
                            .flatMap(defaultCategoryUid -> {
                                log.debug("Using default category: {}", defaultCategoryUid);

                                return goalService.getOrCreateSavingsGoal(accountUid)
                                        .flatMap(savingsGoal -> {
                                            log.debug("Using savings goal: {}", savingsGoal.savingsGoalUid());

                                            LocalDateTime now = LocalDateTime.now();
                                            LocalDateTime weekAgo = now.minus(7, ChronoUnit.DAYS);
                                            log.debug("Fetching transactions from {} to {}", weekAgo, now);

                                            return transactionFeedItemService.getFeedItemsForDateRange(
                                                    accountUid,
                                                    defaultCategoryUid,
                                                    weekAgo,
                                                    now
                                            ).collectList()
                                                    .flatMap(feedItems -> {
                                                        log.debug("Found {} transactions for processing", feedItems.size());

                                                        long totalRoundUp = transactionFeedItemService.calculateRoundUpAmount(feedItems);
                                                        log.info("Calculated total round-up amount: {}", totalRoundUp);

                                                        if (totalRoundUp > 0) {
                                                            return accountService.getEffectiveBalance(accountUid)
                                                                    .flatMap(balance -> {
                                                                        log.debug("Current account balance: {}", balance.minorUnits());

                                                                        if (balance.minorUnits() >= totalRoundUp) {
                                                                            log.info("Transferring {} to savings goal {}", totalRoundUp, savingsGoal.savingsGoalUid());
                                                                            return goalService.transferToSavingsGoal(
                                                                                    accountUid,
                                                                                    UUID.fromString(savingsGoal.savingsGoalUid()),
                                                                                    totalRoundUp
                                                                            ).then();
                                                                        } else {
                                                                            log.warn("Insufficient balance ({}) to transfer round-up amount ({})",
                                                                                    balance.minorUnits(), totalRoundUp);
                                                                            return Mono.error(new InsufficientBalanceException("Insufficient balance to round up"));
                                                                        }
                                                                    });
                                                        } else {
                                                            log.info("No round-up amount to transfer");
                                                            return Mono.empty();
                                                        }
                                                    });
                                        });
                            });
                });
    }
}
