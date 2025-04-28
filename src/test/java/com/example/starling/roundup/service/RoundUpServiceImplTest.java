package com.example.starling.roundup.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.starling.roundup.exception.InsufficientBalanceException;
import com.example.starling.roundup.model.Account;
import com.example.starling.roundup.model.CurrencyAndAmount;
import com.example.starling.roundup.model.FeedItem;
import com.example.starling.roundup.model.SavingsGoal;

@ExtendWith(MockitoExtension.class)
class RoundUpServiceImplTest {

    @Mock
    private TransactionFeedItemService transactionFeedItemService;

    @Mock
    private GoalService goalService;

    @Mock
    private AccountService accountService;

    private RoundUpServiceImpl roundUpService;

    @BeforeEach
    void setUp() {
        roundUpService = new RoundUpServiceImpl(transactionFeedItemService, goalService, accountService);
    }

    @Test
    void roundUpTransactions_noRoundUp_noTransfer() {
        UUID accountUid = UUID.randomUUID();
        UUID defaultCategory = UUID.randomUUID();
        Account account = new Account(accountUid, defaultCategory, "ANY", "GBP");
        List<FeedItem> feedItems = Collections.emptyList();
        String savingsGoalId = UUID.randomUUID().toString();
        CurrencyAndAmount amount = new CurrencyAndAmount("GBP", 0L);

        when(accountService.getDefaultAccount())
                .thenReturn(account);
        when(accountService.getDefaultCategory(account))
                .thenReturn(defaultCategory);
        when(goalService.getOrCreateSavingsGoal(accountUid))
                .thenReturn(new SavingsGoal(savingsGoalId, "GOAL", "GBP", amount));
        when(transactionFeedItemService.getFeedItemsForDateRange(eq(accountUid), eq(defaultCategory), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(feedItems);
        when(transactionFeedItemService.calculateRoundUpAmount(feedItems))
                .thenReturn(0L);

        roundUpService.roundUpTransactions();

        verify(goalService, never()).transferToSavingsGoal(any(), any(), anyLong());
    }

    @Test
    void roundUpTransactions_sufficientBalance_transfersToSavingsGoal() {
        UUID accountUid = UUID.randomUUID();
        UUID defaultCategory = UUID.randomUUID();
        Account account = new Account(accountUid, defaultCategory, "ANY", "GBP");
        String savingsGoalId = UUID.randomUUID().toString();
        long roundUpAmount = 100L;
        List<FeedItem> feedItems = Collections.singletonList(mock(FeedItem.class));
        CurrencyAndAmount balance = new CurrencyAndAmount("GBP", roundUpAmount + 50);

        when(accountService.getDefaultAccount())
                .thenReturn(account);
        when(accountService.getDefaultCategory(account))
                .thenReturn(defaultCategory);
        when(goalService.getOrCreateSavingsGoal(accountUid))
                .thenReturn(new SavingsGoal(savingsGoalId, "GOAL", "GBP", balance));
        when(transactionFeedItemService.getFeedItemsForDateRange(eq(accountUid), eq(defaultCategory), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(feedItems);
        when(transactionFeedItemService.calculateRoundUpAmount(feedItems))
                .thenReturn(roundUpAmount);
        when(accountService.getEffectiveBalance(accountUid))
                .thenReturn(balance);

        roundUpService.roundUpTransactions();

        verify(goalService).transferToSavingsGoal(accountUid, UUID.fromString(savingsGoalId), roundUpAmount);
    }

    @Test
    void roundUpTransactions_insufficientBalance_throwsException() {
        UUID accountUid = UUID.randomUUID();
        UUID defaultCategory = UUID.randomUUID();
        Account account = new Account(accountUid, defaultCategory, "ANY", "GBP");
        String savingsGoalId = UUID.randomUUID().toString();
        long roundUpAmount = 200L;
        List<FeedItem> feedItems = Collections.singletonList(mock(FeedItem.class));
        CurrencyAndAmount balance = new CurrencyAndAmount("GBP", roundUpAmount - 1L);

        when(accountService.getDefaultAccount())
                .thenReturn(account);
        when(accountService.getDefaultCategory(account))
                .thenReturn(defaultCategory);
        when(goalService.getOrCreateSavingsGoal(accountUid))
                .thenReturn(new SavingsGoal(savingsGoalId, "GOAL", "GBP", balance));
        when(transactionFeedItemService.getFeedItemsForDateRange(eq(accountUid), eq(defaultCategory), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(feedItems);
        when(transactionFeedItemService.calculateRoundUpAmount(feedItems))
                .thenReturn(roundUpAmount);
        when(accountService.getEffectiveBalance(accountUid))
                .thenReturn(balance);

        InsufficientBalanceException exception = assertThrows(InsufficientBalanceException.class, () -> roundUpService.roundUpTransactions());
        assertEquals("Insufficient balance to round up", exception.getMessage());

        verify(goalService, never()).transferToSavingsGoal(any(), any(), anyLong());
    }
}
