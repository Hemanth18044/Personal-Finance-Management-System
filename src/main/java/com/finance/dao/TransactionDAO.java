package com.finance.dao;

import com.finance.model.Transaction;
import com.finance.model.TransactionType;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Data Access Object for financial Transactions.
 * Implements CRUD operations with JDBC PreparedStatements and Foreign Key integrity.
 * Supports historical past-month queries, current period calculations, and future/next-month planned expenses.
 */
public class TransactionDAO {

    // In-memory fallback list for offline/demo operation
    private static final List<Transaction> MEMORY_TRANSACTIONS = new CopyOnWriteArrayList<>();
    private static final AtomicInteger TX_ID_GENERATOR = new AtomicInteger(100);

    static {
        LocalDate today = LocalDate.now();
        LocalDate lastMonth = today.minusMonths(1);
        LocalDate nextMonth = today.plusMonths(1);

        // --- PAST MONTH TRANSACTIONS (e.g. October) ---
        Date past1 = Date.valueOf(lastMonth.withDayOfMonth(3));
        Date past2 = Date.valueOf(lastMonth.withDayOfMonth(10));
        Date past3 = Date.valueOf(lastMonth.withDayOfMonth(18));
        Date past4 = Date.valueOf(lastMonth.withDayOfMonth(25));
        MEMORY_TRANSACTIONS.add(new Transaction(1001, 1, 4500.00, TransactionType.INCOME, "Salary", "October engineering salary", past1, new Timestamp(past1.getTime())));
        MEMORY_TRANSACTIONS.add(new Transaction(1002, 1, 1200.00, TransactionType.EXPENSE, "Housing & Rent", "October rent payment", past1, new Timestamp(past1.getTime())));
        MEMORY_TRANSACTIONS.add(new Transaction(1003, 1, 380.00, TransactionType.EXPENSE, "Groceries", "October supermarket grocery", past2, new Timestamp(past2.getTime())));
        MEMORY_TRANSACTIONS.add(new Transaction(1004, 1, 110.00, TransactionType.EXPENSE, "Utilities & Bills", "October power and internet", past3, new Timestamp(past3.getTime())));
        MEMORY_TRANSACTIONS.add(new Transaction(1005, 1, 240.00, TransactionType.EXPENSE, "Dining Out", "October team dinners", past4, new Timestamp(past4.getTime())));

        // --- CURRENT MONTH TRANSACTIONS (e.g. November) ---
        Date cur1 = Date.valueOf(today.withDayOfMonth(1));
        Date cur2 = Date.valueOf(today.withDayOfMonth(Math.min(5, today.lengthOfMonth())));
        Date cur3 = Date.valueOf(today.withDayOfMonth(Math.min(8, today.lengthOfMonth())));
        Date cur4 = Date.valueOf(today.withDayOfMonth(Math.min(12, today.lengthOfMonth())));
        Date curToday = Date.valueOf(today);
        MEMORY_TRANSACTIONS.add(new Transaction(101, 1, 4800.00, TransactionType.INCOME, "Salary", "Current monthly salary", cur1, new Timestamp(cur1.getTime())));
        MEMORY_TRANSACTIONS.add(new Transaction(102, 1, 1250.00, TransactionType.EXPENSE, "Housing & Rent", "Current apartment rent", cur1, new Timestamp(cur1.getTime())));
        MEMORY_TRANSACTIONS.add(new Transaction(103, 1, 340.00, TransactionType.EXPENSE, "Groceries", "Weekly grocery replenishment", cur2, new Timestamp(cur2.getTime())));
        MEMORY_TRANSACTIONS.add(new Transaction(104, 1, 135.00, TransactionType.EXPENSE, "Utilities & Bills", "Fiber broadband & electricity", cur3, new Timestamp(cur3.getTime())));
        MEMORY_TRANSACTIONS.add(new Transaction(105, 1, 750.00, TransactionType.INCOME, "Freelance", "Web design contract milestone", cur4, new Timestamp(cur4.getTime())));
        MEMORY_TRANSACTIONS.add(new Transaction(106, 1, 95.00, TransactionType.EXPENSE, "Dining Out", "Family weekend restaurant", curToday, new Timestamp(curToday.getTime())));

        // --- NEXT MONTH / FUTURE PLANNED EXPENSES (e.g. December) ---
        Date next1 = Date.valueOf(nextMonth.withDayOfMonth(1));
        Date next2 = Date.valueOf(nextMonth.withDayOfMonth(10));
        Date next3 = Date.valueOf(nextMonth.withDayOfMonth(15));
        Date next4 = Date.valueOf(nextMonth.withDayOfMonth(22));
        MEMORY_TRANSACTIONS.add(new Transaction(201, 1, 1250.00, TransactionType.EXPENSE, "Housing & Rent", "Planned: Next month apartment rent", next1, new Timestamp(next1.getTime())));
        MEMORY_TRANSACTIONS.add(new Transaction(202, 1, 180.00, TransactionType.EXPENSE, "Utilities & Bills", "Planned: Winter utility heating estimate", next2, new Timestamp(next2.getTime())));
        MEMORY_TRANSACTIONS.add(new Transaction(203, 1, 300.00, TransactionType.EXPENSE, "Travel", "Planned: Holiday train tickets", next3, new Timestamp(next3.getTime())));
        MEMORY_TRANSACTIONS.add(new Transaction(204, 1, 450.00, TransactionType.EXPENSE, "Shopping", "Planned: Year-end family gift budget", next4, new Timestamp(next4.getTime())));
        MEMORY_TRANSACTIONS.add(new Transaction(205, 1, 4800.00, TransactionType.INCOME, "Salary", "Expected: Next month salary payout", next1, new Timestamp(next1.getTime())));
    }

    /**
     * Inserts a new transaction into the database using PreparedStatement.
     */
    public boolean addTransaction(Transaction tx) {
        if (DBConnection.isLiveDatabase()) {
            String sql = "INSERT INTO transactions (user_id, amount, type, category, description, transaction_date) " +
                         "VALUES (?, ?, ?, ?, ?, ?)";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                stmt.setInt(1, tx.getUserId());
                stmt.setDouble(2, tx.getAmount());
                stmt.setString(3, tx.getType().name());
                stmt.setString(4, tx.getCategory());
                stmt.setString(5, tx.getDescription());
                stmt.setDate(6, tx.getTransactionDate());

                int affected = stmt.executeUpdate();
                if (affected > 0) {
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            tx.setId(rs.getInt(1));
                        }
                    }
                    return true;
                }
            } catch (SQLException e) {
                System.err.println("[TransactionDAO] SQL Exception during addTransaction: " + e.getMessage());
            }
            return false;
        } else {
            tx.setId(TX_ID_GENERATOR.incrementAndGet());
            tx.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            MEMORY_TRANSACTIONS.add(0, tx); // insert at beginning
            return true;
        }
    }

    /**
     * Retrieves all transactions belonging to a specific user, ordered by date descending.
     */
    public List<Transaction> getTransactionsByUserId(int userId) {
        List<Transaction> list = new ArrayList<>();
        if (DBConnection.isLiveDatabase()) {
            String sql = "SELECT id, user_id, amount, type, category, description, transaction_date, created_at " +
                         "FROM transactions WHERE user_id = ? ORDER BY transaction_date DESC, id DESC";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapResultSetToTransaction(rs));
                    }
                }
            } catch (SQLException e) {
                System.err.println("[TransactionDAO] SQL Exception in getTransactionsByUserId: " + e.getMessage());
            }
        } else {
            for (Transaction tx : MEMORY_TRANSACTIONS) {
                if (tx.getUserId() == userId) {
                    list.add(tx);
                }
            }
        }
        return list;
    }

    /**
     * Retrieves transactions for a specific user filtered by calendar year and month (1..12).
     */
    public List<Transaction> getTransactionsByMonth(int userId, int year, int month) {
        List<Transaction> list = new ArrayList<>();
        if (DBConnection.isLiveDatabase()) {
            String sql = "SELECT id, user_id, amount, type, category, description, transaction_date, created_at " +
                         "FROM transactions WHERE user_id = ? AND YEAR(transaction_date) = ? AND MONTH(transaction_date) = ? " +
                         "ORDER BY transaction_date DESC, id DESC";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, userId);
                stmt.setInt(2, year);
                stmt.setInt(3, month);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapResultSetToTransaction(rs));
                    }
                }
            } catch (SQLException e) {
                System.err.println("[TransactionDAO] SQL Exception in getTransactionsByMonth: " + e.getMessage());
            }
        } else {
            for (Transaction tx : MEMORY_TRANSACTIONS) {
                if (tx.getUserId() == userId && tx.getTransactionDate() != null) {
                    LocalDate ld = tx.getTransactionDate().toLocalDate();
                    if (ld.getYear() == year && ld.getMonthValue() == month) {
                        list.add(tx);
                    }
                }
            }
        }
        return list;
    }

    /**
     * Retrieves future/upcoming transactions (e.g. next month planned expenses).
     */
    public List<Transaction> getUpcomingTransactions(int userId) {
        List<Transaction> list = new ArrayList<>();
        LocalDate today = LocalDate.now();
        Date sqlToday = Date.valueOf(today);

        if (DBConnection.isLiveDatabase()) {
            String sql = "SELECT id, user_id, amount, type, category, description, transaction_date, created_at " +
                         "FROM transactions WHERE user_id = ? AND transaction_date > ? ORDER BY transaction_date ASC, id ASC";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, userId);
                stmt.setDate(2, sqlToday);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapResultSetToTransaction(rs));
                    }
                }
            } catch (SQLException e) {
                System.err.println("[TransactionDAO] SQL Exception in getUpcomingTransactions: " + e.getMessage());
            }
        } else {
            for (Transaction tx : MEMORY_TRANSACTIONS) {
                if (tx.getUserId() == userId && tx.isUpcoming()) {
                    list.add(tx);
                }
            }
        }
        return list;
    }

    /**
     * Retrieves historical past transactions (up to and including today).
     */
    public List<Transaction> getPastTransactions(int userId) {
        List<Transaction> list = new ArrayList<>();
        LocalDate today = LocalDate.now();
        Date sqlToday = Date.valueOf(today);

        if (DBConnection.isLiveDatabase()) {
            String sql = "SELECT id, user_id, amount, type, category, description, transaction_date, created_at " +
                         "FROM transactions WHERE user_id = ? AND transaction_date <= ? ORDER BY transaction_date DESC, id DESC";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, userId);
                stmt.setDate(2, sqlToday);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapResultSetToTransaction(rs));
                    }
                }
            } catch (SQLException e) {
                System.err.println("[TransactionDAO] SQL Exception in getPastTransactions: " + e.getMessage());
            }
        } else {
            for (Transaction tx : MEMORY_TRANSACTIONS) {
                if (tx.getUserId() == userId && !tx.isUpcoming()) {
                    list.add(tx);
                }
            }
        }
        return list;
    }

    /**
     * Deletes a transaction safely verifying ownership with Foreign Key/User ID integrity.
     */
    public boolean deleteTransaction(int transactionId, int userId) {
        if (DBConnection.isLiveDatabase()) {
            String sql = "DELETE FROM transactions WHERE id = ? AND user_id = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, transactionId);
                stmt.setInt(2, userId);

                int affected = stmt.executeUpdate();
                return affected > 0;
            } catch (SQLException e) {
                System.err.println("[TransactionDAO] SQL Exception in deleteTransaction: " + e.getMessage());
            }
            return false;
        } else {
            return MEMORY_TRANSACTIONS.removeIf(tx -> tx.getId() == transactionId && tx.getUserId() == userId);
        }
    }

    /**
     * Retrieves transactions filtered by category for a specific user.
     */
    public List<Transaction> getTransactionsByCategory(int userId, String category) {
        List<Transaction> list = new ArrayList<>();
        if (DBConnection.isLiveDatabase()) {
            String sql = "SELECT id, user_id, amount, type, category, description, transaction_date, created_at " +
                         "FROM transactions WHERE user_id = ? AND category = ? ORDER BY transaction_date DESC";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, userId);
                stmt.setString(2, category);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        list.add(mapResultSetToTransaction(rs));
                    }
                }
            } catch (SQLException e) {
                System.err.println("[TransactionDAO] SQL Exception in getTransactionsByCategory: " + e.getMessage());
            }
        } else {
            for (Transaction tx : MEMORY_TRANSACTIONS) {
                if (tx.getUserId() == userId && tx.getCategory().equalsIgnoreCase(category)) {
                    list.add(tx);
                }
            }
        }
        return list;
    }

    /**
     * Calculates total for a user and transaction type across all dates.
     */
    public double calculateTotalByType(int userId, TransactionType type) {
        if (DBConnection.isLiveDatabase()) {
            String sql = "SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE user_id = ? AND type = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, userId);
                stmt.setString(2, type.name());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble(1);
                    }
                }
            } catch (SQLException e) {
                System.err.println("[TransactionDAO] SQL Exception in calculateTotalByType: " + e.getMessage());
            }
            return 0.0;
        } else {
            double total = 0.0;
            for (Transaction tx : MEMORY_TRANSACTIONS) {
                if (tx.getUserId() == userId && tx.getType() == type) {
                    total += tx.getAmount();
                }
            }
            return total;
        }
    }

    /**
     * Calculates total for a specific month and year.
     */
    public double calculateTotalByMonthAndType(int userId, int year, int month, TransactionType type) {
        if (DBConnection.isLiveDatabase()) {
            String sql = "SELECT COALESCE(SUM(amount), 0.0) FROM transactions " +
                         "WHERE user_id = ? AND YEAR(transaction_date) = ? AND MONTH(transaction_date) = ? AND type = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, userId);
                stmt.setInt(2, year);
                stmt.setInt(3, month);
                stmt.setString(4, type.name());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble(1);
                    }
                }
            } catch (SQLException e) {
                System.err.println("[TransactionDAO] SQL Exception in calculateTotalByMonthAndType: " + e.getMessage());
            }
            return 0.0;
        } else {
            double total = 0.0;
            for (Transaction tx : MEMORY_TRANSACTIONS) {
                if (tx.getUserId() == userId && tx.getType() == type && tx.getTransactionDate() != null) {
                    LocalDate ld = tx.getTransactionDate().toLocalDate();
                    if (ld.getYear() == year && ld.getMonthValue() == month) {
                        total += tx.getAmount();
                    }
                }
            }
            return total;
        }
    }

    /**
     * Calculates total upcoming amount (e.g. planned expenses or expected income).
     */
    public double calculateUpcomingTotal(int userId, TransactionType type) {
        LocalDate today = LocalDate.now();
        Date sqlToday = Date.valueOf(today);

        if (DBConnection.isLiveDatabase()) {
            String sql = "SELECT COALESCE(SUM(amount), 0.0) FROM transactions " +
                         "WHERE user_id = ? AND transaction_date > ? AND type = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, userId);
                stmt.setDate(2, sqlToday);
                stmt.setString(3, type.name());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble(1);
                    }
                }
            } catch (SQLException e) {
                System.err.println("[TransactionDAO] SQL Exception in calculateUpcomingTotal: " + e.getMessage());
            }
            return 0.0;
        } else {
            double total = 0.0;
            for (Transaction tx : MEMORY_TRANSACTIONS) {
                if (tx.getUserId() == userId && tx.getType() == type && tx.isUpcoming()) {
                    total += tx.getAmount();
                }
            }
            return total;
        }
    }

    private Transaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        return new Transaction(
                rs.getInt("id"),
                rs.getInt("user_id"),
                rs.getDouble("amount"),
                TransactionType.fromString(rs.getString("type")),
                rs.getString("category"),
                rs.getString("description"),
                rs.getDate("transaction_date"),
                rs.getTimestamp("created_at")
        );
    }
}
