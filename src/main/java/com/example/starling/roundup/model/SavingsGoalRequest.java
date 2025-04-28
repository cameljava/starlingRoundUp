package com.example.starling.roundup.model;

public record SavingsGoalRequest(
    String name,
    String currency,
    CurrencyAndAmount target
) {}
