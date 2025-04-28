package com.example.starling.roundup.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

import org.springframework.web.util.UriComponentsBuilder;

import com.example.starling.roundup.model.FeedItem;

/**
 * Utility class providing common functionality for the Starling Bank Round-Up service.
 * Contains methods for date formatting, URL building, and calculating round-up amounts.
 * <p>
 * This class is not meant to be instantiated.
 */
public final class Utils {

    private static final int PENCE_PER_POUND = 100;
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private Utils() {
        throw new AssertionError("Utils class should not be instantiated");
    }

    /**
     * Converts a LocalDateTime to an ISO-8601 formatted string in UTC timezone.
     * 
     * @param date the date to convert, must not be null
     * @return the formatted date string in UTC timezone
     * @throws NullPointerException if date is null
     */
    public static String dateToString(LocalDateTime date) {
        Objects.requireNonNull(date, "Date must not be null");
        return date.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
                .format(ISO_FORMATTER);
    }

    /**
     * Builds a URL for the Starling Bank transaction API with the specified parameters.
     * Uses Spring's UriComponentsBuilder to ensure proper URI encoding.
     * 
     * @param accountUUID the account UUID, must not be null
     * @param categoryId the category UUID, must not be null
     * @param from the start date for transactions, must not be null
     * @param to the end date for transactions, must not be null
     * @return the formatted URL string
     * @throws NullPointerException if any parameter is null
     */
    public static String buildTransactionUrl(UUID accountUUID, UUID categoryId, LocalDateTime from, LocalDateTime to) {
        Objects.requireNonNull(accountUUID, "Account UUID must not be null");
        Objects.requireNonNull(categoryId, "Category ID must not be null");
        Objects.requireNonNull(from, "From date must not be null");
        Objects.requireNonNull(to, "To date must not be null");
        return UriComponentsBuilder
                .fromPath("/api/v2/feed/account/{accountUid}/category/{categoryId}/transactions-between")
                .queryParam("minTransactionTimestamp", java.net.URLEncoder.encode(dateToString(from), java.nio.charset.StandardCharsets.UTF_8))
                .queryParam("maxTransactionTimestamp", java.net.URLEncoder.encode(dateToString(to), java.nio.charset.StandardCharsets.UTF_8))
                .buildAndExpand(accountUUID.toString(), categoryId.toString())
                .toUriString();
    }

    /**
     * Calculates the round-up amount for a transaction, which is the difference between
     * the transaction amount and the next whole pound (100 pence).
     * <p>
     * Examples:
     * <ul>
     *   <li>£4.50 (450 pence) rounds up to £5.00 (500 pence), so the round-up is 50 pence.</li>
     *   <li>£5.00 (500 pence) is already a whole pound, so the round-up is 0 pence.</li>
     *   <li>£0.01 (1 penny) rounds up to £1.00 (100 pence), so the round-up is 99 pence.</li>
     * </ul>
     * 
     * @param item the feed item containing the transaction, must not be null
     * @return the round-up amount in minor units (pence)
     * @throws NullPointerException if item is null
     */
    public static long calculateItemRoundUp(FeedItem item) {
        Objects.requireNonNull(item, "Feed item must not be null");
        Objects.requireNonNull(item.amount(), "Feed item amount must not be null");
        
        long minorUnits = item.amount().minorUnits();
        // Only process positive (outgoing) transactions
        if (minorUnits <= 0) {
            return 0L;
        }
        
        long roundUpToNearestPound = ((minorUnits + PENCE_PER_POUND - 1) / PENCE_PER_POUND) * PENCE_PER_POUND;
        return roundUpToNearestPound - minorUnits;
    }
}
