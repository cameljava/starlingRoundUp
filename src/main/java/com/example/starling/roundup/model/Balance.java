package com.example.starling.roundup.model;

/**
 * Model for account balance including cleared and effective balances.
 */
public record Balance(
    CurrencyAndAmount clearedBalance,
    CurrencyAndAmount effectiveBalance,
    CurrencyAndAmount pendingTransactions,
    CurrencyAndAmount acceptedOverdraft,
    CurrencyAndAmount amount
) {}
