package com.example.starling.roundup.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.example.starling.roundup.model.FeedItem;

import reactor.core.publisher.Flux;

/**
 * Service interface for fetching transaction feed items and calculating
 * round-up amounts.
 */
public interface TransactionFeedItemService {

    /**
     * Retrieves transaction feed items for the given account and category
     * between the specified dates.
     *
     * @param accountUUID the UUID of the account
     * @param categoryId the UUID of the category
     * @param from the start date-time (inclusive)
     * @param to the end date-time (inclusive)
     * @return Flux<FeedItem> containing the stream of FeedItem objects
     */
    Flux<FeedItem> getFeedItemsForDateRange(UUID accountUUID, UUID categoryId, LocalDateTime from, LocalDateTime to);

    /**
     * Calculates the total round-up amount from a list of feed items.
     *
     * @param feedItems list of FeedItem objects
     * @return total round-up amount in minor currency units
     */
    long calculateRoundUpAmount(List<FeedItem> feedItems);
}
