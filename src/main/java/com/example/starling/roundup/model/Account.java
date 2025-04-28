package com.example.starling.roundup.model;

import java.util.UUID;

public record Account(
    UUID accountUid,
    UUID defaultCategory,
    String accountType,
    String currency
) {}
