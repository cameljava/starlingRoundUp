package com.example.starling.roundup.service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import com.example.starling.roundup.model.CurrencyAndAmount;
import com.example.starling.roundup.model.FeedItem;
import com.example.starling.roundup.model.FeedItems;
import com.example.starling.roundup.util.Utils;

@ExtendWith(MockitoExtension.class)
class TransactionFeedItemServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    private TransactionFeedItemService transactionFeedItemService;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        transactionFeedItemService = new TransactionFeedItemServiceImpl(restTemplate);
    }

    @Test
    void getFeedItemsForDateRange_Success() {
        // Given
        UUID accountUUID = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();

        CurrencyAndAmount amount = new CurrencyAndAmount("GBP", 100L);
        FeedItem feedItem = createFeedItem(amount);
        List<FeedItem> expectedItems = Collections.singletonList(feedItem);

        FeedItems feedItems = new FeedItems(expectedItems);

        // Build the URL the same way as the implementation would
        String url = Utils.buildTransactionUrl(accountUUID, categoryId, from, to);

        when(restTemplate.getForObject(eq(url), eq(FeedItems.class)))
                .thenReturn(feedItems);

        // When
        List<FeedItem> result = transactionFeedItemService.getFeedItemsForDateRange(accountUUID, categoryId, from, to);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).amount().minorUnits());
    }

    @Test
    void getFeedItemsForDateRange_ReturnsEmptyList_WhenResponseIsNull() {
        // Given
        when(restTemplate.getForObject(any(String.class), eq(FeedItems.class)))
                .thenReturn(null);

        // When
        List<FeedItem> result = transactionFeedItemService.getFeedItemsForDateRange(
                UUID.randomUUID(), UUID.randomUUID(), LocalDateTime.now(), LocalDateTime.now());

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void calculateRoundUpAmount_Success() {
        // Given
        List<FeedItem> feedItems = Arrays.asList(
            createFeedItem(new CurrencyAndAmount("GBP", 435L)), // Round up 65p to £5
            createFeedItem(new CurrencyAndAmount("GBP", 520L)), // Round up 80p to £6
            createFeedItem(new CurrencyAndAmount("GBP", 87L))  // Round up 13p to £1
        );

        // When
        long result = transactionFeedItemService.calculateRoundUpAmount(feedItems);

        // Then
        // Expected round ups: 65p + 80p + 13p = 158
        assertEquals(158L, result);
    }
    @Test
    void calculateRoundUpAmount_SuccessWhenAmountIsAlreadyWholeNumber() {
        // Given
        List<FeedItem> feedItems = Arrays.asList(
            createFeedItem(new CurrencyAndAmount("GBP", 500L)), 
            createFeedItem(new CurrencyAndAmount("GBP", 0L)), 
            createFeedItem(new CurrencyAndAmount("GBP", 800L))
        );

        // When
        long result = transactionFeedItemService.calculateRoundUpAmount(feedItems);

        // Then
        // Expected round ups: 0 
        assertEquals(0L, result);
    }

    @Test
    void calculateRoundUpAmount_ReturnsZero_WhenFeedItemsIsEmpty() {
        // When
        long result = transactionFeedItemService.calculateRoundUpAmount(Collections.emptyList());

        // Then
        assertEquals(0L, result);
    }

    private FeedItem createFeedItem(CurrencyAndAmount amount) {
        return new FeedItem(
            UUID.randomUUID(),
            UUID.randomUUID(),
            amount,
            amount,
            "OUT",
            LocalDateTime.now(),
            LocalDateTime.now(),
            LocalDateTime.now(),
            "MASTER_CARD",
            "SETTLED"
        );
    }
}
