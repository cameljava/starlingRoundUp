package com.example.starling.roundup.model;

public record SavingsGoal(
    String savingsGoalUid,
    String name,
    String currency,
    CurrencyAndAmount target
) {}
