package com.finance.dao;

import com.finance.model.User;
import com.finance.util.PasswordHasher;

import java.sql.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Data Access Object for User entity.
 * Implements CRUD operations with JDBC PreparedStatements and SQL Injection prevention.
 */
public class UserDAO {

    // In-memory fallback repository for offline/demo operation
    private static final Map<String, User> MEMORY_USERS = new ConcurrentHashMap<>();
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(10);

    static {
        // Pre-seed demo users
        User john = new User(1, "john_doe", PasswordHasher.hashPassword("password123"),
                "John Doe", new Timestamp(System.currentTimeMillis()));
        User sarah = new User(2, "sarah_connor", PasswordHasher.hashPassword("password123"),
                "Sarah Connor", new Timestamp(System.currentTimeMillis()));
        MEMORY_USERS.put(john.getUsername().toLowerCase(), john);
        MEMORY_USERS.put(sarah.getUsername().toLowerCase(), sarah);
    }

    /**
     * Registers a new user in the database using PreparedStatement.
     */
    public boolean register(User user) {
        if (DBConnection.isLiveDatabase()) {
            String sql = "INSERT INTO users (username, password, full_name) VALUES (?, ?, ?)";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                stmt.setString(1, user.getUsername());
                stmt.setString(2, user.getPassword());
                stmt.setString(3, user.getFullName());

                int affected = stmt.executeUpdate();
                if (affected > 0) {
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            user.setId(rs.getInt(1));
                        }
                    }
                    return true;
                }
            } catch (SQLException e) {
                System.err.println("[UserDAO] SQL Exception during user registration: " + e.getMessage());
            }
            return false;
        } else {
            // Offline fallback
            if (isUsernameTaken(user.getUsername())) {
                return false;
            }
            user.setId(ID_GENERATOR.incrementAndGet());
            user.setCreatedAt(new Timestamp(System.currentTimeMillis()));
            MEMORY_USERS.put(user.getUsername().toLowerCase(), user);
            return true;
        }
    }

    /**
     * Finds a user by unique username using parameterized PreparedStatement.
     */
    public User findByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }

        if (DBConnection.isLiveDatabase()) {
            String sql = "SELECT id, username, password, full_name, created_at FROM users WHERE username = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, username.trim());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToUser(rs);
                    }
                }
            } catch (SQLException e) {
                System.err.println("[UserDAO] SQL Exception in findByUsername: " + e.getMessage());
            }
            return null;
        } else {
            return MEMORY_USERS.get(username.trim().toLowerCase());
        }
    }

    /**
     * Finds a user by primary key ID.
     */
    public User findById(int id) {
        if (DBConnection.isLiveDatabase()) {
            String sql = "SELECT id, username, password, full_name, created_at FROM users WHERE id = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, id);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapResultSetToUser(rs);
                    }
                }
            } catch (SQLException e) {
                System.err.println("[UserDAO] SQL Exception in findById: " + e.getMessage());
            }
            return null;
        } else {
            for (User u : MEMORY_USERS.values()) {
                if (u.getId() == id) return u;
            }
            return null;
        }
    }

    /**
     * Checks if a username is already taken.
     */
    public boolean isUsernameTaken(String username) {
        if (username == null) return false;
        if (DBConnection.isLiveDatabase()) {
            String sql = "SELECT COUNT(*) FROM users WHERE username = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, username.trim());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1) > 0;
                    }
                }
            } catch (SQLException e) {
                System.err.println("[UserDAO] SQL Exception in isUsernameTaken: " + e.getMessage());
            }
            return false;
        } else {
            return MEMORY_USERS.containsKey(username.trim().toLowerCase());
        }
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getInt("id"),
                rs.getString("username"),
                rs.getString("password"),
                rs.getString("full_name"),
                rs.getTimestamp("created_at")
        );
    }
}
