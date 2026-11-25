package com.finance;

import com.finance.model.BudgetAlert;
import com.finance.model.Transaction;
import com.finance.model.TransactionType;
import com.finance.model.User;
import com.finance.service.FinanceService;
import com.finance.service.SessionManager;
import com.finance.service.UserService;
import com.finance.util.PasswordHasher;
import com.finance.view.TransactionTableModel;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class QuickVerificationTest {
    public static void main(String[] args) {
        System.out.println(">>> STARTING AUTOMATED TEST SUITE (BUDGETS & ALERTS) <<<");

        // 1. Test PasswordHasher
        System.out.print("[Test 1] PasswordHasher: ");
        String hash = PasswordHasher.hashPassword("secret123");
        assert PasswordHasher.verifyPassword("secret123", hash);
        assert !PasswordHasher.verifyPassword("wrongpass", hash);
        System.out.println("PASSED");

        // 2. Test UserService & SessionManager
        System.out.print("[Test 2] UserService & SessionManager: ");
        UserService userService = new UserService();
        User loggedUser = userService.login("john_doe", "password123");
        assert loggedUser != null : "john_doe login failed";
        assert SessionManager.getInstance().isLoggedIn() : "Session not active";
        assert SessionManager.getInstance().getCurrentUser().getUsername().equals("john_doe");
        System.out.println("PASSED (User authenticated: " + loggedUser.getFullName() + ")");

        // 3. Test Multi-Month Analytics
        System.out.print("[Test 3] Multi-Month Calculations: ");
        FinanceService financeService = new FinanceService();
        int userId = loggedUser.getId();

        LocalDate today = LocalDate.now();
        LocalDate lastMonth = today.minusMonths(1);
        LocalDate nextMonth = today.plusMonths(1);

        List<Transaction> pastTx = financeService.getTransactionsByMonth(userId, lastMonth.getYear(), lastMonth.getMonthValue());
        List<Transaction> curTx = financeService.getTransactionsByMonth(userId, today.getYear(), today.getMonthValue());
        List<Transaction> nextTx = financeService.getTransactionsByMonth(userId, nextMonth.getYear(), nextMonth.getMonthValue());

        assert !pastTx.isEmpty() : "Past month transactions empty";
        assert !curTx.isEmpty() : "Current month transactions empty";
        assert !nextTx.isEmpty() : "Next month planned transactions empty";
        System.out.println("PASSED (Past: " + pastTx.size() + ", Current: " + curTx.size() + ", Next Planned: " + nextTx.size() + ")");

        // 4. Test Category Budgeting and 80%/100% Alert Thresholds
        System.out.print("[Test 4] Category Budgeting & Alerts (80% and 100%): ");
        // Set a $500 monthly limit for Food
        financeService.setBudget(userId, "Food", 500.00);
        Double foodBudget = financeService.getBudget(userId, "Food");
        assert foodBudget != null && foodBudget == 500.00 : "Budget limit setting failed";

        Date txDate = Date.valueOf(today.withDayOfMonth(today.lengthOfMonth())); // ensure current month

        // Add $350 expense (70% - should be NONE)
        BudgetAlert alert1 = financeService.checkBudgetAlert(userId, "Food", txDate, 350.00);
        assert alert1.getLevel() == BudgetAlert.Level.NONE : "Expected NONE at 70%";

        // Add $420 expense (84% - should be WARNING)
        BudgetAlert alert2 = financeService.checkBudgetAlert(userId, "Food", txDate, 420.00);
        assert alert2.getLevel() == BudgetAlert.Level.WARNING : "Expected WARNING at 84%";
        assert alert2.getMessage().contains("84%") : "Warning message missing percentage";

        // Add $550 expense (110% - should be EXCEEDED)
        BudgetAlert alert3 = financeService.checkBudgetAlert(userId, "Food", txDate, 550.00);
        assert alert3.getLevel() == BudgetAlert.Level.EXCEEDED : "Expected EXCEEDED at 110%";
        assert alert3.getMessage().contains("exceeded") : "Alert message missing exceeded keyword";
        System.out.println("PASSED (80% Warning & 100% Alert Thresholds verified)");

        // 5. Test JTable Data Binding Model with Status Pill
        System.out.print("[Test 5] JTable Data Binding: ");
        List<Transaction> allTx = financeService.getUserTransactions(userId);
        TransactionTableModel model = new TransactionTableModel(allTx);
        assert model.getRowCount() == allTx.size() : "Model row count mismatch";
        assert model.getColumnCount() == 7 : "Model column count mismatch";
        System.out.println("PASSED (Bound " + model.getRowCount() + " rows with status badges)");

        System.out.println(">>> ALL 5 CORE ENGINE & BUDGET ALERT TESTS PASSED SUCCESSFULLY! <<<");
    }
}
