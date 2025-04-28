package com.example.starling.roundup.util;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import com.example.starling.roundup.model.CurrencyAndAmount;
import com.example.starling.roundup.model.FeedItem;

class UtilsTest {

    private static FeedItem createFeedItem(long amountInPence) {
        return new FeedItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new CurrencyAndAmount("GBP", amountInPence),
                new CurrencyAndAmount("GBP", amountInPence),
                "OUT",
                LocalDateTime.now(),
                LocalDateTime.now(),
                LocalDateTime.now(),
                "CARD",
                "SETTLED"
        );
    }

    @Test
    void dateToString_FormatsDateCorrectly() {
        // Given
        LocalDateTime date = LocalDateTime.of(2025, Month.JULY, 15, 11, 30, 45, 123000000);

        // When
        String result = Utils.dateToString(date);

        // Then
        assertEquals("2025-07-15T01:30:45.123Z", result);
    }

    @Test
    void dateToString_FormatsDateCorrectly_daylightSaving() {
        // Sydney daylight saving: First Sudney of October - First Sunday of April
        // Given
        LocalDateTime date = LocalDateTime.of(2025, Month.MARCH, 15, 11, 30, 45, 123000000);

        // When
        String result = Utils.dateToString(date);

        // Then
        assertEquals("2025-03-15T00:30:45.123Z", result);
    }

    @Test
    void buildTransactionUrl_FormatsUrlCorrectly() {
        // Given
        UUID accountUUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID categoryId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        LocalDateTime from = LocalDateTime.of(2025, Month.MAY, 15, 11, 0);
        LocalDateTime to = LocalDateTime.of(2025, Month.MAY, 15, 18, 0);

        // When
        String result = Utils.buildTransactionUrl(accountUUID, categoryId, from, to);

        // Then
        // URL-encoded colons (:) are %3A, periods (.) are %2E
        String expectedUrl = "/api/v2/feed/account/11111111-1111-1111-1111-111111111111/category/22222222-2222-2222-2222-222222222222/transactions-between"
                + "?minTransactionTimestamp=2025-05-15T01%3A00%3A00.000Z&maxTransactionTimestamp=2025-05-15T08%3A00%3A00.000Z";
        assertEquals(expectedUrl, result);
    }

    @Test
    void calculateItemRoundUp_RoundsUpToNearestPound() {
        // Given
        FeedItem feedItem = createFeedItem(450L); // £4.50

        // When
        long result = Utils.calculateItemRoundUp(feedItem);

        // Then
        assertEquals(50L, result); // Should round up to £5.00, so difference is 50p
    }

    @Test
    void calculateItemRoundUp_HandlesWholeNumbers() {
        // Given
        FeedItem feedItem = createFeedItem(500L); // £5.00

        // When
        long result = Utils.calculateItemRoundUp(feedItem);

        // Then
        assertEquals(0L, result); // Already a whole number, no rounding needed
    }

    @Test
    void calculateItemRoundUp_HandlesZero() {
        // Given
        FeedItem feedItem = createFeedItem(0L); // £0.00

        // When
        long result = Utils.calculateItemRoundUp(feedItem);

        // Then
        assertEquals(0L, result); // Already a whole number, no rounding needed
    }

    @Test
    void calculateItemRoundUp_HandlesNegativeValues() {
        // Given
        FeedItem feedItem = createFeedItem(-100L); // -£1.00

        // When
        long result = Utils.calculateItemRoundUp(feedItem);

        // Then
        assertEquals(0L, result); // Negative values should be treated as 0
    }

    @Test
    void calculateItemRoundUp_HandlesOnePenny() {
        // Given
        FeedItem feedItem = createFeedItem(1L); // £0.01

        // When
        long result = Utils.calculateItemRoundUp(feedItem);

        // Then
        assertEquals(99L, result); // Should round up to £1.00
    }

    @Test
    void calculateItemRoundUp_ThrowsException_WhenItemIsNull() {
        // When/Then
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> Utils.calculateItemRoundUp(null));
        assertEquals("Feed item must not be null", exception.getMessage());
    }

    @Test
    void dateToString_ThrowsException_WhenDateIsNull() {
        // When/Then
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> Utils.dateToString(null));
        assertEquals("Date must not be null", exception.getMessage());
    }

    @Test
    void buildTransactionUrl_ThrowsException_WhenParamsAreNull() {
        UUID accountUUID = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        NullPointerException exception1 = assertThrows(NullPointerException.class,
                () -> Utils.buildTransactionUrl(null, categoryId, now, now));
        assertEquals("Account UUID must not be null", exception1.getMessage());

        NullPointerException exception2 = assertThrows(NullPointerException.class,
                () -> Utils.buildTransactionUrl(accountUUID, null, now, now));
        assertEquals("Category ID must not be null", exception2.getMessage());

        NullPointerException exception3 = assertThrows(NullPointerException.class,
                () -> Utils.buildTransactionUrl(accountUUID, categoryId, null, now));
        assertEquals("From date must not be null", exception3.getMessage());

        NullPointerException exception4 = assertThrows(NullPointerException.class,
                () -> Utils.buildTransactionUrl(accountUUID, categoryId, now, null));
        assertEquals("To date must not be null", exception4.getMessage());
    }

    @Test
    void buildTransactionUrl_HandlesSpecialCharacters() {
        // Given
        UUID accountUUID = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID categoryId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        // Create a date with a specific format to test URL encoding
        LocalDateTime date = LocalDateTime.of(2025, Month.JUNE, 15, 12, 30, 45, 500000000);

        // When
        String result = Utils.buildTransactionUrl(accountUUID, categoryId, date, date);

        // Then
        // Verify that the URL contains properly encoded timestamps
        String encodedDate = result.substring(result.indexOf("?") + "minTransactionTimestamp=".length(),
                result.indexOf("&maxTransactionTimestamp"));
        // Should contain URL-encoded characters
        assertTrue(encodedDate.contains("%3A")); // Encoded colon

        // Should not contain any unencoded special characters
        assertFalse(encodedDate.contains(":"));
    }
}
