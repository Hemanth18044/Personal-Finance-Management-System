package com.finance.dao;

import com.finance.model.Budget;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Data Access Object for Budget entities.
 * Supports MySQL upsert operations and in-memory demo persistence.
 */
public class BudgetDAO {

    private static final Map<String, Double> MEMORY_BUDGETS = new ConcurrentHashMap<>();

    static {
        // Pre-seed default monthly budgets for User 1
        MEMORY_BUDGETS.put("1:groceries", 500.00);
        MEMORY_BUDGETS.put("1:food", 500.00);
        MEMORY_BUDGETS.put("1:dining out", 250.00);
        MEMORY_BUDGETS.put("1:housing & rent", 1300.00);
        MEMORY_BUDGETS.put("1:utilities & bills", 200.00);
        MEMORY_BUDGETS.put("1:shopping", 300.00);
        MEMORY_BUDGETS.put("1:travel", 400.00);
    }

    /**
     * Sets or updates a category budget limit for a user.
     */
    public boolean setBudget(int userId, String category, double monthlyLimit) {
        if (category == null || category.trim().isEmpty() || monthlyLimit <= 0) {
            return false;
        }
        String cleanCat = category.trim();

        if (DBConnection.isLiveDatabase()) {
            String sql = "INSERT INTO budgets (user_id, category, monthly_limit) VALUES (?, ?, ?) " +
                         "ON DUPLICATE KEY UPDATE monthly_limit = VALUES(monthly_limit)";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, userId);
                stmt.setString(2, cleanCat);
                stmt.setDouble(3, monthlyLimit);

                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                System.err.println("[BudgetDAO] SQL Exception in setBudget: " + e.getMessage());
            }
            return false;
        } else {
            MEMORY_BUDGETS.put(userId + ":" + cleanCat.toLowerCase(), monthlyLimit);
            return true;
        }
    }

    /**
     * Gets the monthly limit for a specific category, or null if none set.
     */
    public Double getBudget(int userId, String category) {
        if (category == null) return null;
        String cleanCat = category.trim();

        if (DBConnection.isLiveDatabase()) {
            String sql = "SELECT monthly_limit FROM budgets WHERE user_id = ? AND LOWER(category) = LOWER(?)";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, userId);
                stmt.setString(2, cleanCat);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble("monthly_limit");
                    }
                }
            } catch (SQLException e) {
                System.err.println("[BudgetDAO] SQL Exception in getBudget: " + e.getMessage());
            }
            return null;
        } else {
            return MEMORY_BUDGETS.get(userId + ":" + cleanCat.toLowerCase());
        }
    }

    /**
     * Retrieves all configured category budgets for a user.
     */
    public Map<String, Double> getAllBudgets(int userId) {
        Map<String, Double> map = new LinkedHashMap<>();

        if (DBConnection.isLiveDatabase()) {
            String sql = "SELECT category, monthly_limit FROM budgets WHERE user_id = ? ORDER BY category ASC";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, userId);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        map.put(rs.getString("category"), rs.getDouble("monthly_limit"));
                    }
                }
            } catch (SQLException e) {
                System.err.println("[BudgetDAO] SQL Exception in getAllBudgets: " + e.getMessage());
            }
        } else {
            String prefix = userId + ":";
            for (Map.Entry<String, Double> e : MEMORY_BUDGETS.entrySet()) {
                if (e.getKey().startsWith(prefix)) {
                    String cat = e.getKey().substring(prefix.length());
                    // Capitalize nicely
                    String capitalized = cat.substring(0, 1).toUpperCase() + cat.substring(1);
                    map.put(capitalized, e.getValue());
                }
            }
        }
        return map;
    }
}
