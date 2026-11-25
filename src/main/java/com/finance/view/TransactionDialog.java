package com.finance.view;

import com.finance.model.BudgetAlert;
import com.finance.model.TransactionType;
import com.finance.service.FinanceService;
import com.finance.util.CurrencyFormatter;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.sql.Date;
import java.time.LocalDate;

/**
 * Modern modal dialog for capturing transactions.
 * Features high-contrast custom buttons and quick date presets.
 */
public class TransactionDialog extends JDialog {

    private final int userId;
    private final FinanceService financeService;
    private final Runnable onSuccessCallback;

    private JComboBox<TransactionType> typeCombo;
    private JComboBox<String> categoryCombo;
    private JTextField amountField;
    private JTextField dateField;
    private JTextField descriptionField;
    private JLabel statusNoticeLabel;

    private static final String[] INCOME_CATEGORIES = {
            "Salary", "Freelance & Consulting", "Investments & Dividends", "Bonus", "Rental Income", "Gifts", "Other Income"
    };

    private static final String[] EXPENSE_CATEGORIES = {
            "Housing & Rent", "Groceries", "Utilities & Bills", "Software & Subscriptions",
            "Dining Out", "Transportation & Fuel", "Healthcare", "Shopping & Gifts",
            "Travel & Vacation", "Education", "Insurance", "Miscellaneous"
    };

    public TransactionDialog(Frame parent, int userId, FinanceService financeService, Runnable onSuccessCallback) {
        super(parent, "Add Transaction & Budget Planner", true);
        this.userId = userId;
        this.financeService = financeService;
        this.onSuccessCallback = onSuccessCallback;

        initUI();
    }

    private void initUI() {
        setSize(520, 590);
        setLocationRelativeTo(getParent());
        setResizable(false);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(22, 28, 22, 28));
        mainPanel.setBackground(new Color(248, 250, 252));

        // Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("Record Transaction / Planned Budget");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(15, 23, 42));

        JLabel subtitleLabel = new JLabel("Log historical records or schedule next month's upcoming expenses");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(100, 116, 139));

        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        // Form Fields
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        // 1. Transaction Type
        gbc.gridy = 0;
        formPanel.add(createLabel("Transaction Type"), gbc);
        gbc.gridy = 1;
        typeCombo = new JComboBox<>(TransactionType.values());
        typeCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        typeCombo.setPreferredSize(new Dimension(380, 34));
        formPanel.add(typeCombo, gbc);

        // 2. Category
        gbc.gridy = 2;
        formPanel.add(createLabel("Category"), gbc);
        gbc.gridy = 3;
        categoryCombo = new JComboBox<>(EXPENSE_CATEGORIES);
        categoryCombo.setEditable(true);
        categoryCombo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        categoryCombo.setPreferredSize(new Dimension(380, 34));
        formPanel.add(categoryCombo, gbc);

        // 3. Amount
        gbc.gridy = 4;
        formPanel.add(createLabel("Amount ($ USD)"), gbc);
        gbc.gridy = 5;
        amountField = new JTextField(15);
        amountField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        amountField.setPreferredSize(new Dimension(380, 34));
        formPanel.add(amountField, gbc);

        // 4. Date & Quick Presets
        gbc.gridy = 6;
        formPanel.add(createLabel("Date (YYYY-MM-DD)"), gbc);

        gbc.gridy = 7;
        dateField = new JTextField(LocalDate.now().toString(), 15);
        dateField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        dateField.setPreferredSize(new Dimension(380, 34));
        formPanel.add(dateField, gbc);

        // Quick Date Preset Buttons using CustomButton (High Contrast)
        gbc.gridy = 8;
        JPanel presetPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        presetPanel.setOpaque(false);

        CustomButton todayBtn = createPresetButton("Today", e -> setDate(LocalDate.now()));
        CustomButton nextMonthBtn = createPresetButton("Next Month (1st)", e -> setDate(LocalDate.now().plusMonths(1).withDayOfMonth(1)));
        CustomButton nextMonthEndBtn = createPresetButton("Next Month (End)", e -> {
            LocalDate nm = LocalDate.now().plusMonths(1);
            setDate(nm.withDayOfMonth(nm.lengthOfMonth()));
        });
        CustomButton pastMonthBtn = createPresetButton("Past Month", e -> setDate(LocalDate.now().minusMonths(1).withDayOfMonth(15)));

        presetPanel.add(todayBtn);
        presetPanel.add(nextMonthBtn);
        presetPanel.add(nextMonthEndBtn);
        presetPanel.add(pastMonthBtn);
        formPanel.add(presetPanel, gbc);

        // Dynamic notice banner
        gbc.gridy = 9;
        statusNoticeLabel = new JLabel("● Logged as: Current/Past Settled Transaction");
        statusNoticeLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        statusNoticeLabel.setForeground(new Color(22, 101, 52));
        formPanel.add(statusNoticeLabel, gbc);

        // 5. Description
        gbc.gridy = 10;
        formPanel.add(createLabel("Description / Purpose"), gbc);
        gbc.gridy = 11;
        descriptionField = new JTextField(15);
        descriptionField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        descriptionField.setPreferredSize(new Dimension(380, 34));
        formPanel.add(descriptionField, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // Footer Actions with CustomButtons
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footerPanel.setOpaque(false);

        CustomButton cancelBtn = new CustomButton("Cancel", new Color(241, 245, 249), new Color(71, 85, 105), 8);
        cancelBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cancelBtn.setPreferredSize(new Dimension(90, 36));
        cancelBtn.addActionListener(e -> dispose());

        CustomButton saveBtn = new CustomButton("Save Record", new Color(37, 99, 235), Color.WHITE, 8);
        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        saveBtn.setPreferredSize(new Dimension(130, 36));
        saveBtn.setHoverBg(new Color(29, 78, 216));
        saveBtn.addActionListener(e -> saveTransaction());

        footerPanel.add(cancelBtn);
        footerPanel.add(saveBtn);
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);

        // Event Listeners
        typeCombo.addActionListener(e -> {
            TransactionType selected = (TransactionType) typeCombo.getSelectedItem();
            categoryCombo.removeAllItems();
            String[] list = selected == TransactionType.INCOME ? INCOME_CATEGORIES : EXPENSE_CATEGORIES;
            for (String cat : list) {
                categoryCombo.addItem(cat);
            }
        });

        dateField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                updateDateNotice();
            }
        });
    }

    private CustomButton createPresetButton(String text, java.awt.event.ActionListener al) {
        CustomButton btn = new CustomButton(text, new Color(241, 245, 249), new Color(30, 41, 59), 6);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        btn.setHoverBg(new Color(226, 232, 240));
        btn.addActionListener(al);
        return btn;
    }

    private void setDate(LocalDate date) {
        dateField.setText(date.toString());
        updateDateNotice();
    }

    private void updateDateNotice() {
        try {
            LocalDate entered = LocalDate.parse(dateField.getText().trim());
            if (entered.isAfter(LocalDate.now())) {
                statusNoticeLabel.setText("✦ Future Date: Will be marked as Planned/Upcoming Expense");
                statusNoticeLabel.setForeground(new Color(124, 58, 237)); // Violet
            } else {
                statusNoticeLabel.setText("● Settled: Historical/Current transaction");
                statusNoticeLabel.setForeground(new Color(22, 101, 52)); // Green
            }
        } catch (Exception ex) {
            statusNoticeLabel.setText("⚠️ Invalid date format (use YYYY-MM-DD)");
            statusNoticeLabel.setForeground(new Color(220, 38, 38));
        }
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(new Color(51, 65, 85));
        return label;
    }

    private void saveTransaction() {
        try {
            TransactionType type = (TransactionType) typeCombo.getSelectedItem();
            Object catObj = categoryCombo.getSelectedItem();
            String category = catObj != null ? catObj.toString().trim() : "";
            String amountStr = amountField.getText().trim();
            String dateStr = dateField.getText().trim();
            String description = descriptionField.getText().trim();

            if (category.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please specify or select a category.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
                if (amount <= 0.0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Please enter a valid positive numerical amount.", "Validation Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            Date date = CurrencyFormatter.parseDate(dateStr);
            BudgetAlert alert = financeService.addTransactionWithAlert(userId, amount, type, category, description, date);

            if (alert != null && alert.hasAlert()) {
                if (alert.getLevel() == BudgetAlert.Level.EXCEEDED) {
                    JOptionPane.showMessageDialog(this,
                            alert.getMessage(),
                            "🚨 Alert: Budget Exceeded!",
                            JOptionPane.ERROR_MESSAGE);
                } else if (alert.getLevel() == BudgetAlert.Level.WARNING) {
                    JOptionPane.showMessageDialog(this,
                            alert.getMessage(),
                            "⚠️ Warning: 80% Budget Limit Reached",
                            JOptionPane.WARNING_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(this, "Transaction saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }

            if (onSuccessCallback != null) {
                onSuccessCallback.run();
            }
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error saving record: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
