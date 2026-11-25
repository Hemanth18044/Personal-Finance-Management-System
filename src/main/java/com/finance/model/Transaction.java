package com.finance.model;

import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;

/**
 * Model entity representing a financial transaction (Income or Expense).
 * Supports past historical records, current activity, and future/next-month planned expenses.
 */
public class Transaction {
    private int id;
    private int userId;
    private double amount;
    private TransactionType type;
    private String category;
    private String description;
    private Date transactionDate;
    private Timestamp createdAt;

    public Transaction() {
    }

    public Transaction(int id, int userId, double amount, TransactionType type, String category,
                       String description, Date transactionDate, Timestamp createdAt) {
        this.id = id;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.description = description;
        this.transactionDate = transactionDate;
        this.createdAt = createdAt;
    }

    public Transaction(int userId, double amount, TransactionType type, String category,
                       String description, Date transactionDate) {
        this.userId = userId;
        this.amount = amount;
        this.type = type;
        this.category = category;
        this.description = description;
        this.transactionDate = transactionDate;
    }

    /**
     * Checks if this transaction is scheduled for the future (e.g. next month's planned expenses).
     */
    public boolean isUpcoming() {
        if (transactionDate == null) return false;
        LocalDate txLocal = transactionDate.toLocalDate();
        LocalDate today = LocalDate.now();
        return txLocal.isAfter(today);
    }

    /**
     * Returns a human-friendly status string for web-style badge rendering.
     */
    public String getStatus() {
        return isUpcoming() ? "Planned / Upcoming" : "Settled";
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "id=" + id +
                ", userId=" + userId +
                ", amount=" + amount +
                ", type=" + type +
                ", category='" + category + '\'' +
                ", description='" + description + '\'' +
                ", transactionDate=" + transactionDate +
                ", status=" + getStatus() +
                '}';
    }
}
