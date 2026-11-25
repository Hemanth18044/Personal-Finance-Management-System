package com.finance.service;

import com.finance.dao.UserDAO;
import com.finance.model.User;
import com.finance.util.PasswordHasher;

/**
 * Service handling user authentication and registration business logic.
 */
public class UserService {

    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;
    }

    /**
     * Authenticates a user by username and password.
     * Uses SHA-256 hashing to compare with stored credential.
     */
    public User login(String username, String rawPassword) throws IllegalArgumentException {
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("Username cannot be empty");
        }
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }

        User user = userDAO.findByUsername(username.trim());
        if (user == null) {
            return null; // User not found
        }

        if (PasswordHasher.verifyPassword(rawPassword, user.getPassword())) {
            SessionManager.getInstance().startSession(user);
            return user;
        }
        return null; // Invalid password
    }

    /**
     * Registers a new user with input validation and password hashing.
     */
    public boolean register(String username, String password, String fullName) throws IllegalArgumentException {
        if (username == null || username.trim().length() < 3) {
            throw new IllegalArgumentException("Username must be at least 3 characters long");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long");
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            throw new IllegalArgumentException("Full name cannot be empty");
        }

        if (userDAO.isUsernameTaken(username.trim())) {
            throw new IllegalArgumentException("Username '" + username.trim() + "' is already taken");
        }

        String hashedPassword = PasswordHasher.hashPassword(password);
        User newUser = new User(username.trim(), hashedPassword, fullName.trim());
        return userDAO.register(newUser);
    }

    /**
     * Logs out the current user by terminating the active session.
     */
    public void logout() {
        SessionManager.getInstance().endSession();
    }
}
