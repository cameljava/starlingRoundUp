package com.example.starling.roundup.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.example.starling.roundup.model.FeedItem;
import com.example.starling.roundup.model.FeedItems;
import com.example.starling.roundup.util.Utils;

/**
 * Implementation of the TransactionFeedItemService interface that provides
 * functionality for fetching transaction data from the Starling Bank API and
 * calculating round-up amounts.
 * <p>
 * This service is responsible for:
 * <ul>
 *   <li>Retrieving transactions from a specific account and category within a date range</li>
 *   <li>Calculating the total round-up amount for a collection of transactions</li>
 * </ul>
 * <p>
 * Round-up calculation is delegated to the Utils class to maintain separation of concerns.
 * <p>
 * This service uses RestTemplate for API communication and includes appropriate error 
 * handling and logging for observability.
 * 
 * @see TransactionFeedItemService
 * @see Utils#calculateItemRoundUp(FeedItem)
 * @see Utils#buildTransactionUrl(UUID, UUID, LocalDateTime, LocalDateTime)
 */
@Service
public class TransactionFeedItemServiceImpl implements TransactionFeedItemService {
    private static final Logger log = LoggerFactory.getLogger(TransactionFeedItemServiceImpl.class);
    
    private final RestTemplate restTemplate;

    /**
     * Constructs a new TransactionFeedItemServiceImpl with the specified RestTemplate.
     * 
     * @param restTemplate the RestTemplate to use for API communication
     */
    public TransactionFeedItemServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Builds a URL for the Starling Bank transaction API using Utils.buildTransactionUrl</li>
     *   <li>Handles null API responses gracefully by returning an empty list</li>
     *   <li>Relies on the configured error handler in RestTemplate to handle HTTP exceptions</li>
     * </ul>
     * <p>
     * Error cases:
     * <ul>
     *   <li>If the API returns a null response, an empty list is returned</li>
     *   <li>If HTTP errors occur, they are handled by the RestTemplate error handler</li>
     * </ul>
     * 
     * @throws com.example.starling.roundup.exception.DownstreamClientException if there's a client-side error with the API
     * @throws com.example.starling.roundup.exception.DownstreamServerException if there's a server-side error with the API
     */
    @Override
    public List<FeedItem> getFeedItemsForDateRange(UUID accountUUID, UUID categoryId, LocalDateTime from, LocalDateTime to) {
        String url = Utils.buildTransactionUrl(accountUUID, categoryId, from, to);
        log.debug("Fetching transactions for account {} and category {} from {} to {}", accountUUID, categoryId, from, to);
        
        // Custom error handler in RestTemplate will convert any API errors to appropriate exceptions
        // GlobalExceptionHandler will then handle these exceptions
        return Optional.ofNullable(restTemplate.getForObject(url, FeedItems.class))
                .map(FeedItems::feedItems)
                .orElse(Collections.emptyList());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Implementation details:
     * <ul>
     *   <li>Uses Java Streams to process each feed item</li>
     *   <li>Delegates the individual round-up calculation to Utils.calculateItemRoundUp</li>
     *   <li>Handles empty lists gracefully by returning zero</li>
     * </ul>
     * <p>
     * The calculation rounds each transaction up to the nearest pound and sums these round-up values.
     * For example:
     * <pre>
     *    £2.35 transaction -> round up to £3.00 -> round-up amount = £0.65
     *    £5.00 transaction -> round up to £5.00 -> round-up amount = £0.00
     * </pre>
     */
    @Override
    public long calculateRoundUpAmount(List<FeedItem> feedItems) {
        long roundUpAmount = feedItems.stream()
                .mapToLong(Utils::calculateItemRoundUp)
                .sum();
        
        log.debug("Calculated round-up amount: {} from {} transactions", roundUpAmount, feedItems.size());
        return roundUpAmount;
    }
}
