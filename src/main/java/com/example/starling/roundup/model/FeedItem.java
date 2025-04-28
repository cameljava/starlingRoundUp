package com.example.starling.roundup.model;

import java.time.LocalDateTime;
import java.util.UUID;

public record FeedItem(
    UUID feedItemUid,
    UUID categoryUid,
    CurrencyAndAmount amount,
    CurrencyAndAmount sourceAmount,
    String direction,
    LocalDateTime updatedAt,
    LocalDateTime transactionTime,
    LocalDateTime settlementTime,
    String source,
    String status
) {}