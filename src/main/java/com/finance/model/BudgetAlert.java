package com.finance.model;

/**
 * Encapsulates real-time budget threshold warnings and alerts.
 */
public class BudgetAlert {

    public enum Level {
        NONE,
        WARNING, // 80% to 99%
        EXCEEDED // 100%+
    }

    private final Level level;
    private final String category;
    private final double limit;
    private final double spent;
    private final double percentage;
    private final String message;

    public BudgetAlert(Level level, String category, double limit, double spent, double percentage, String message) {
        this.level = level;
        this.category = category;
        this.limit = limit;
        this.spent = spent;
        this.percentage = percentage;
        this.message = message;
    }

    public Level getLevel() {
        return level;
    }

    public String getCategory() {
        return category;
    }

    public double getLimit() {
        return limit;
    }

    public double getSpent() {
        return spent;
    }

    public double getPercentage() {
        return percentage;
    }

    public String getMessage() {
        return message;
    }

    public boolean hasAlert() {
        return level != Level.NONE;
    }

    @Override
    public String toString() {
        return "BudgetAlert{" +
                "level=" + level +
                ", category='" + category + '\'' +
                ", limit=" + limit +
                ", spent=" + spent +
                ", percentage=" + percentage + "%" +
                ", message='" + message + '\'' +
                '}';
    }
}
