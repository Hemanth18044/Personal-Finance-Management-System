package com.finance.view;

import com.finance.service.UserService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Registration View: Account Creation Frame for Personal Finance Management System.
 */
public class RegisterFrame extends JFrame {

    private final UserService userService;
    private final LoginFrame loginFrame;

    private JTextField fullNameField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    private CustomButton registerButton;
    private JButton backButton;

    public RegisterFrame(LoginFrame loginFrame) {
        this.loginFrame = loginFrame;
        this.userService = new UserService();
        initUI();
    }

    private void initUI() {
        setTitle("Personal Finance Manager - Create Account");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(440, 560);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(25, 30, 25, 30));
        mainPanel.setBackground(new Color(245, 247, 250));

        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Create Account");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        titleLabel.setForeground(new Color(33, 37, 41));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Start tracking your finances today");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitleLabel.setForeground(new Color(108, 117, 125));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(titleLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitleLabel);
        headerPanel.add(Box.createVerticalStrut(15));

        // Form Fields
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        // Full Name
        gbc.gridy = 0;
        formPanel.add(createLabel("Full Name"), gbc);
        gbc.gridy = 1;
        fullNameField = new JTextField(18);
        fullNameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formPanel.add(fullNameField, gbc);

        // Username
        gbc.gridy = 2;
        formPanel.add(createLabel("Username"), gbc);
        gbc.gridy = 3;
        usernameField = new JTextField(18);
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formPanel.add(usernameField, gbc);

        // Password
        gbc.gridy = 4;
        formPanel.add(createLabel("Password (min 6 characters)"), gbc);
        gbc.gridy = 5;
        passwordField = new JPasswordField(18);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formPanel.add(passwordField, gbc);

        // Confirm Password
        gbc.gridy = 6;
        formPanel.add(createLabel("Confirm Password"), gbc);
        gbc.gridy = 7;
        confirmPasswordField = new JPasswordField(18);
        confirmPasswordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        formPanel.add(confirmPasswordField, gbc);

        // Buttons
        JPanel actionPanel = new JPanel();
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.Y_AXIS));
        actionPanel.setOpaque(false);

        registerButton = new CustomButton("Register", new Color(22, 101, 52), Color.WHITE, 8);
        registerButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        registerButton.setPreferredSize(new Dimension(300, 40));
        registerButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        registerButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        backButton = new JButton("Already have an account? Log In");
        backButton.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        backButton.setForeground(new Color(13, 110, 253));
        backButton.setContentAreaFilled(false);
        backButton.setBorderPainted(false);
        backButton.setFocusPainted(false);
        backButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        backButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        actionPanel.add(registerButton);
        actionPanel.add(Box.createVerticalStrut(8));
        actionPanel.add(backButton);

        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(actionPanel, BorderLayout.SOUTH);
        setContentPane(mainPanel);

        // Action Listeners
        registerButton.addActionListener(e -> performRegister());
        backButton.addActionListener(e -> returnToLogin());
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(new Color(73, 80, 87));
        return label;
    }

    private void performRegister() {
        String fullName = fullNameField.getText();
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());

        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this,
                    "Passwords do not match. Please re-enter.",
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            boolean success = userService.register(username, password, fullName);
            if (success) {
                JOptionPane.showMessageDialog(this,
                        "Registration successful! You may now sign in.",
                        "Success",
                        JOptionPane.INFORMATION_MESSAGE);
                returnToLogin();
            } else {
                JOptionPane.showMessageDialog(this,
                        "Could not complete registration. Please try another username.",
                        "Registration Failed",
                        JOptionPane.ERROR_MESSAGE);
            }
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this,
                    ex.getMessage(),
                    "Validation Error",
                    JOptionPane.WARNING_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Registration error: " + ex.getMessage(),
                    "System Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void returnToLogin() {
        if (loginFrame != null) {
            loginFrame.setVisible(true);
        }
        this.dispose();
    }
}
