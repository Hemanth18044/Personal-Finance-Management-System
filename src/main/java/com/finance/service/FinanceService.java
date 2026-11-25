package com.finance.service;

import com.finance.dao.BudgetDAO;
import com.finance.dao.TransactionDAO;
import com.finance.model.BudgetAlert;
import com.finance.model.Transaction;
import com.finance.model.TransactionType;
import com.finance.util.CurrencyFormatter;

import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service managing financial transactions, multi-month navigation,
 * planned upcoming expenses, category budgets, and real-time alert thresholds.
 */
public class FinanceService {

    private final TransactionDAO transactionDAO;
    private final BudgetDAO budgetDAO;

    public FinanceService() {
        this.transactionDAO = new TransactionDAO();
        this.budgetDAO = new BudgetDAO();
    }

    public FinanceService(TransactionDAO transactionDAO, BudgetDAO budgetDAO) {
        this.transactionDAO = transactionDAO;
        this.budgetDAO = budgetDAO;
    }

    /**
     * Records a new transaction after validating business constraints.
     */
    public boolean addTransaction(int userId, double amount, TransactionType type,
                                  String category, String description, Date transactionDate) {
        if (amount <= 0.0) {
            throw new IllegalArgumentException("Transaction amount must be strictly greater than zero");
        }
        if (type == null) {
            throw new IllegalArgumentException("Transaction type must be INCOME or EXPENSE");
        }
        if (category == null || category.trim().isEmpty()) {
            throw new IllegalArgumentException("Category cannot be empty");
        }
        if (transactionDate == null) {
            transactionDate = new Date(System.currentTimeMillis());
        }

        Transaction tx = new Transaction(userId, amount, type, category.trim(),
                description != null ? description.trim() : "", transactionDate);
        return transactionDAO.addTransaction(tx);
    }

    /**
     * Records a transaction and evaluates real-time budget thresholds (80% warning, 100% alert).
     */
    public BudgetAlert addTransactionWithAlert(int userId, double amount, TransactionType type,
                                              String category, String description, Date transactionDate) {
        if (transactionDate == null) {
            transactionDate = new Date(System.currentTimeMillis());
        }

        BudgetAlert alert = null;
        if (type == TransactionType.EXPENSE) {
            alert = checkBudgetAlert(userId, category, transactionDate, amount);
        }

        boolean success = addTransaction(userId, amount, type, category, description, transactionDate);
        if (!success) {
            throw new RuntimeException("Failed to save transaction record");
        }

        return alert;
    }

    /**
     * Checks if adding an expense triggers the 80% warning or 100% exceeded alert.
     */
    public BudgetAlert checkBudgetAlert(int userId, String category, Date date, double newAmount) {
        if (category == null || category.trim().isEmpty() || newAmount <= 0) {
            return new BudgetAlert(BudgetAlert.Level.NONE, category, 0, 0, 0, "");
        }
        String cleanCat = category.trim();
        Double limit = budgetDAO.getBudget(userId, cleanCat);

        if (limit == null || limit <= 0) {
            return new BudgetAlert(BudgetAlert.Level.NONE, cleanCat, 0, 0, 0, "");
        }

        LocalDate ld = (date != null) ? date.toLocalDate() : LocalDate.now();
        double currentSpend = getCategoryMonthSpend(userId, cleanCat, ld.getYear(), ld.getMonthValue());
        double projectedSpend = currentSpend + newAmount;
        double pct = (projectedSpend / limit) * 100.0;

        if (pct >= 100.0) {
            String msg = "Alert: You have exceeded your " + cleanCat + " budget! (Spent: " +
                    CurrencyFormatter.formatCurrency(projectedSpend) + " of " + CurrencyFormatter.formatCurrency(limit) + ")";
            return new BudgetAlert(BudgetAlert.Level.EXCEEDED, cleanCat, limit, projectedSpend, pct, msg);
        } else if (pct >= 80.0) {
            String msg = "Warning: You have used " + (int) Math.round(pct) + "% of your " + cleanCat + " budget. (Spent: " +
                    CurrencyFormatter.formatCurrency(projectedSpend) + " of " + CurrencyFormatter.formatCurrency(limit) + ")";
            return new BudgetAlert(BudgetAlert.Level.WARNING, cleanCat, limit, projectedSpend, pct, msg);
        }

        return new BudgetAlert(BudgetAlert.Level.NONE, cleanCat, limit, projectedSpend, pct, "Budget OK");
    }

    /**
     * Sets or updates monthly budget limit for a category.
     */
    public boolean setBudget(int userId, String category, double monthlyLimit) {
        return budgetDAO.setBudget(userId, category, monthlyLimit);
    }

    /**
     * Gets monthly budget limit for a category.
     */
    public Double getBudget(int userId, String category) {
        return budgetDAO.getBudget(userId, category);
    }

    /**
     * Gets all category budgets configured for a user.
     */
    public Map<String, Double> getAllBudgets(int userId) {
        return budgetDAO.getAllBudgets(userId);
    }

    /**
     * Calculates total spent in a specific category for a given month.
     */
    public double getCategoryMonthSpend(int userId, String category, int year, int month) {
        List<Transaction> list = getTransactionsByMonth(userId, year, month);
        double total = 0.0;
        for (Transaction tx : list) {
            if (tx.getType() == TransactionType.EXPENSE && tx.getCategory().equalsIgnoreCase(category)) {
                total += tx.getAmount();
            }
        }
        return total;
    }

    /**
     * Retrieves all transactions belonging to a specific user.
     */
    public List<Transaction> getUserTransactions(int userId) {
        return transactionDAO.getTransactionsByUserId(userId);
    }

    /**
     * Retrieves transactions for a specific month and year.
     */
    public List<Transaction> getTransactionsByMonth(int userId, int year, int month) {
        return transactionDAO.getTransactionsByMonth(userId, year, month);
    }

    /**
     * Retrieves future/upcoming transactions (e.g. next month planned expenses).
     */
    public List<Transaction> getUpcomingTransactions(int userId) {
        return transactionDAO.getUpcomingTransactions(userId);
    }

    /**
     * Retrieves settled historical transactions (up to today).
     */
    public List<Transaction> getPastTransactions(int userId) {
        return transactionDAO.getPastTransactions(userId);
    }

    /**
     * Deletes a transaction, ensuring the user owns the record.
     */
    public boolean deleteTransaction(int transactionId, int userId) {
        return transactionDAO.deleteTransaction(transactionId, userId);
    }

    /**
     * Calculates the all-time net balance for a user.
     */
    public double calculateBalance(int userId) {
        double income = calculateTotalByType(userId, TransactionType.INCOME);
        double expense = calculateTotalByType(userId, TransactionType.EXPENSE);
        return income - expense;
    }

    /**
     * Calculates monthly net balance for a specific year and month.
     */
    public double calculateMonthBalance(int userId, int year, int month) {
        double income = transactionDAO.calculateTotalByMonthAndType(userId, year, month, TransactionType.INCOME);
        double expense = transactionDAO.calculateTotalByMonthAndType(userId, year, month, TransactionType.EXPENSE);
        return income - expense;
    }

    /**
     * Calculates monthly income for a specific year and month.
     */
    public double calculateMonthIncome(int userId, int year, int month) {
        return transactionDAO.calculateTotalByMonthAndType(userId, year, month, TransactionType.INCOME);
    }

    /**
     * Calculates monthly expense for a specific year and month.
     */
    public double calculateMonthExpense(int userId, int year, int month) {
        return transactionDAO.calculateTotalByMonthAndType(userId, year, month, TransactionType.EXPENSE);
    }

    /**
     * Calculates total planned upcoming expenses.
     */
    public double calculateUpcomingExpenses(int userId) {
        return transactionDAO.calculateUpcomingTotal(userId, TransactionType.EXPENSE);
    }

    /**
     * Calculates total expected upcoming income.
     */
    public double calculateUpcomingIncome(int userId) {
        return transactionDAO.calculateUpcomingTotal(userId, TransactionType.INCOME);
    }

    /**
     * Calculates all-time total by type (INCOME or EXPENSE).
     */
    public double calculateTotalByType(int userId, TransactionType type) {
        return transactionDAO.calculateTotalByType(userId, type);
    }

    /**
     * Filters transactions by category for a user.
     */
    public List<Transaction> getTransactionsByCategory(int userId, String category) {
        return transactionDAO.getTransactionsByCategory(userId, category);
    }

    /**
     * Aggregates expenses by category for a given month.
     */
    public Map<String, Double> getCategoryBreakdown(int userId, Integer year, Integer month) {
        List<Transaction> list = (year != null && month != null)
                ? getTransactionsByMonth(userId, year, month)
                : getUserTransactions(userId);

        Map<String, Double> breakdown = new LinkedHashMap<>();
        for (Transaction tx : list) {
            if (tx.getType() == TransactionType.EXPENSE) {
                breakdown.put(tx.getCategory(), breakdown.getOrDefault(tx.getCategory(), 0.0) + tx.getAmount());
            }
        }
        return breakdown;
    }
}
