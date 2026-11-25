package com.finance.dao;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Manages JDBC Database Connection using Singleton pattern.
 * Supports MySQL live connection, with automatic offline fallback detection.
 */
public class DBConnection {

    private static Connection connection = null;
    private static boolean liveDatabase = false;
    private static boolean tested = false;

    private static String url = "jdbc:mysql://localhost:3306/personal_finance_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static String user = "root";
    private static String password = "root";
    private static String driver = "com.mysql.cj.jdbc.Driver";

    static {
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream input = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                url = prop.getProperty("db.url", url);
                user = prop.getProperty("db.user", user);
                password = prop.getProperty("db.password", password);
                driver = prop.getProperty("db.driver", driver);
            }
        } catch (Exception e) {
            System.err.println("[DBConnection] Could not read db.properties, using default credentials: " + e.getMessage());
        }
    }

    /**
     * Obtains the active database connection or tests live availability.
     */
    public static synchronized Connection getConnection() {
        if (!tested) {
            testLiveConnection();
            tested = true;
        }

        try {
            if (liveDatabase && (connection == null || connection.isClosed())) {
                Class.forName(driver);
                connection = DriverManager.getConnection(url, user, password);
            }
        } catch (Exception e) {
            System.err.println("[DBConnection] Failed to reconnect to MySQL: " + e.getMessage());
            liveDatabase = false;
            connection = null;
        }
        return connection;
    }

    private static void testLiveConnection() {
        try {
            Class.forName(driver);
            connection = DriverManager.getConnection(url, user, password);
            liveDatabase = true;
            System.out.println("[DBConnection] Successfully connected to live MySQL database: " + url);
        } catch (ClassNotFoundException e) {
            System.out.println("[DBConnection] MySQL JDBC Driver not found on classpath.");
            System.out.println("[DBConnection] Operating in Offline/Demo Persistence Mode.");
            liveDatabase = false;
        } catch (SQLException e) {
            System.out.println("[DBConnection] MySQL server offline or unreachable (" + e.getMessage() + ").");
            System.out.println("[DBConnection] Operating in Offline/Demo Persistence Mode (All features active).");
            liveDatabase = false;
        }
    }

    public static boolean isLiveDatabase() {
        if (!tested) {
            testLiveConnection();
            tested = true;
        }
        return liveDatabase;
    }

    public static synchronized void closeConnection() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                System.out.println("[DBConnection] MySQL connection closed successfully.");
            } catch (SQLException e) {
                System.err.println("[DBConnection] Error closing connection: " + e.getMessage());
            }
        }
    }
}
