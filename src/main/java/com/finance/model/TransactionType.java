package com.finance.model;

/**
 * Represents the type of financial transaction: INCOME or EXPENSE.
 */
public enum TransactionType {
    INCOME,
    EXPENSE;

    public static TransactionType fromString(String type) {
        if (type == null) {
            throw new IllegalArgumentException("Transaction type cannot be null");
        }
        return TransactionType.valueOf(type.trim().toUpperCase());
    }
}
