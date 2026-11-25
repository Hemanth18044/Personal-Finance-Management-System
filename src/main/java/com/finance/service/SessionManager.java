package com.finance.service;

import com.finance.model.User;

/**
 * Manages the active session for the logged-in user across Swing views.
 * Implements the Singleton pattern to provide global, thread-safe access to session state.
 */
public class SessionManager {

    private static SessionManager instance;
    private User currentUser;

    private SessionManager() {
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public synchronized void startSession(User user) {
        this.currentUser = user;
    }

    public synchronized User getCurrentUser() {
        return currentUser;
    }

    public synchronized boolean isLoggedIn() {
        return currentUser != null;
    }

    public synchronized void endSession() {
        this.currentUser = null;
    }
}
