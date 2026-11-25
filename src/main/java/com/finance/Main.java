package com.finance;

import com.finance.view.LoginFrame;
import com.finance.web.WebServer;

import javax.swing.*;

/**
 * Main application entry point for the Personal Finance Management System.
 * Automatically starts the embedded Web Server at http://localhost:8080
 * and launches the Java Swing & AWT Desktop Application GUI.
 */
public class Main {

    public static void main(String[] args) {
        // 1. Start embedded Web Server for browser localhost access
        try {
            WebServer.start();
        } catch (Exception e) {
            System.err.println("[Main] Could not start embedded web server: " + e.getMessage());
        }

        // 2. Set modern native Look and Feel for Desktop GUI
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.out.println("[Main] Using default Swing Look and Feel: " + e.getMessage());
        }

        // 3. Launch Desktop GUI safely on the Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            try {
                LoginFrame loginFrame = new LoginFrame();
                loginFrame.setVisible(true);
                System.out.println("[Main] Personal Finance Desktop GUI started successfully.");
                System.out.println("-------------------------------------------------------");
                System.out.println("👉 Desktop App: GUI Window active on your screen");
                System.out.println("👉 Localhost Link: http://localhost:8080");
                System.out.println("-------------------------------------------------------");
            } catch (Exception e) {
                System.err.println("[Main] Desktop GUI launch error: " + e.getMessage());
            }
        });
    }
}
