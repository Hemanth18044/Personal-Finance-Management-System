package com.finance.view;

import com.finance.dao.DBConnection;
import com.finance.model.Transaction;
import com.finance.model.TransactionType;
import com.finance.model.User;
import com.finance.service.FinanceService;
import com.finance.service.UserService;
import com.finance.util.CurrencyFormatter;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Modern SaaS Website-Style Dashboard for Personal Finance Management System.
 * Features high-contrast custom buttons, month-by-month switching (past, present, next month),
 * planned expenses tracking, visual category breakdown charts, and live search.
 */
public class DashboardFrame extends JFrame {

    private final User currentUser;
    private final FinanceService financeService;
    private final UserService userService;

    // View state
    private YearMonth selectedMonth;
    private String currentViewMode = "ALL"; // "ALL", "MONTH", "UPCOMING", "PAST"

    // UI Components
    private JLabel balanceValueLabel;
    private JLabel incomeValueLabel;
    private JLabel expenseValueLabel;
    private JLabel upcomingValueLabel;
    private JLabel periodTitleLabel;
    private JLabel monthDisplayLabel;
    private JLabel dbStatusBadge;

    private JTextField searchField;
    private JComboBox<String> categoryFilterCombo;
    private CustomButton deleteButton;
    private CustomButton prevMonthBtn;
    private CustomButton nextMonthBtn;
    private CustomButton addTransactionBtn;

    private JTable transactionTable;
    private TransactionTableModel tableModel;
    private CategoryChartPanel chartPanel;

    private List<Transaction> activeDataSet = new ArrayList<>();

    public DashboardFrame(User user) {
        this.currentUser = user;
        this.financeService = new FinanceService();
        this.userService = new UserService();
        this.selectedMonth = YearMonth.now();

        initUI();
        loadDashboardData();
    }

    private void initUI() {
        setTitle("FinanceOS — Personal Finance & Budget Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1260, 820);
        setMinimumSize(new Dimension(1080, 700));
        setLocationRelativeTo(null);

        // Root container with Sidebar (West) and Main Content (Center)
        JPanel rootPanel = new JPanel(new BorderLayout());
        rootPanel.setBackground(new Color(248, 250, 252));

        rootPanel.add(buildSidebar(), BorderLayout.WEST);
        rootPanel.add(buildMainContent(), BorderLayout.CENTER);

        setContentPane(rootPanel);
    }

    /**
     * Modern SaaS Dark Sidebar with 100% visible, high-contrast buttons
     */
    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel(new BorderLayout());
        sidebar.setPreferredSize(new Dimension(245, 0));
        sidebar.setBackground(new Color(15, 23, 42)); // Deep Navy / Slate 900
        sidebar.setBorder(new EmptyBorder(24, 18, 20, 18));

        // 1. App Brand Header
        JPanel brandPanel = new JPanel();
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setOpaque(false);

        JLabel brandLogo = new JLabel("[•] FinanceOS");
        brandLogo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        brandLogo.setForeground(Color.WHITE);

        JLabel brandBadge = new JLabel("DESKTOP SAAS SUITE");
        brandBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        brandBadge.setForeground(new Color(56, 189, 248)); // Sky blue

        brandPanel.add(brandLogo);
        brandPanel.add(Box.createVerticalStrut(4));
        brandPanel.add(brandBadge);
        brandPanel.add(Box.createVerticalStrut(28));

        // 2. Navigation Items (Using CustomButton to guarantee pure white text on dark background)
        JPanel navMenu = new JPanel();
        navMenu.setLayout(new BoxLayout(navMenu, BoxLayout.Y_AXIS));
        navMenu.setOpaque(false);

        CustomButton navAll = createNavButton("•  All Transactions", "ALL");
        CustomButton navThisMonth = createNavButton("•  Current Month", "THIS_MONTH");
        CustomButton navNextMonth = createNavButton("•  Next Month (Planned)", "UPCOMING");
        CustomButton navPastMonths = createNavButton("•  Past Historical Data", "PAST");

        navMenu.add(navAll);
        navMenu.add(Box.createVerticalStrut(8));
        navMenu.add(navThisMonth);
        navMenu.add(Box.createVerticalStrut(8));
        navMenu.add(navNextMonth);
        navMenu.add(Box.createVerticalStrut(8));
        navMenu.add(navPastMonths);

        sidebar.add(brandPanel, BorderLayout.NORTH);
        sidebar.add(navMenu, BorderLayout.CENTER);

        // 3. User Profile Card at bottom of Sidebar
        JPanel userCard = new JPanel(new BorderLayout(10, 0));
        userCard.setOpaque(false);
        userCard.setBorder(new CompoundBorder(
                new LineBorder(new Color(51, 65, 85), 1, true),
                new EmptyBorder(12, 12, 12, 12)
        ));

        // Avatar Initial
        JLabel avatarLabel = new JLabel(getInitials(currentUser.getFullName()), SwingConstants.CENTER);
        avatarLabel.setPreferredSize(new Dimension(36, 36));
        avatarLabel.setOpaque(true);
        avatarLabel.setBackground(new Color(37, 99, 235));
        avatarLabel.setForeground(Color.WHITE);
        avatarLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));

        // User Details
        JPanel userDetails = new JPanel();
        userDetails.setLayout(new BoxLayout(userDetails, BoxLayout.Y_AXIS));
        userDetails.setOpaque(false);

        JLabel userName = new JLabel(currentUser.getFullName());
        userName.setFont(new Font("Segoe UI", Font.BOLD, 13));
        userName.setForeground(Color.WHITE);

        JLabel userHandle = new JLabel("@" + currentUser.getUsername());
        userHandle.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        userHandle.setForeground(new Color(148, 163, 184));

        userDetails.add(userName);
        userDetails.add(userHandle);

        CustomButton logoutBtn = new CustomButton("Sign Out", new Color(15, 23, 42), new Color(248, 113, 113), 6);
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        logoutBtn.setPreferredSize(new Dimension(68, 28));
        logoutBtn.setHoverBg(new Color(69, 10, 10));
        logoutBtn.addActionListener(e -> performLogout());

        JPanel userBottom = new JPanel(new BorderLayout());
        userBottom.setOpaque(false);
        userBottom.add(userDetails, BorderLayout.WEST);
        userBottom.add(logoutBtn, BorderLayout.EAST);

        userCard.add(avatarLabel, BorderLayout.WEST);
        userCard.add(userBottom, BorderLayout.CENTER);

        sidebar.add(userCard, BorderLayout.SOUTH);
        return sidebar;
    }

    private CustomButton createNavButton(String text, String modeKey) {
        CustomButton btn = new CustomButton(text, new Color(30, 41, 59), Color.WHITE, 8);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        btn.setPreferredSize(new Dimension(205, 42));
        btn.setHoverBg(new Color(51, 65, 85));

        btn.addActionListener(e -> {
            this.currentViewMode = modeKey;
            if ("THIS_MONTH".equals(modeKey)) {
                this.selectedMonth = YearMonth.now();
            } else if ("UPCOMING".equals(modeKey)) {
                this.selectedMonth = YearMonth.now().plusMonths(1);
            } else if ("PAST".equals(modeKey)) {
                this.selectedMonth = YearMonth.now().minusMonths(1);
            }
            loadDashboardData();
        });
        return btn;
    }

    /**
     * Main Dashboard Content Area
     */
    private JPanel buildMainContent() {
        JPanel content = new JPanel(new BorderLayout(0, 18));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(22, 26, 22, 26));

        // 1. Top Bar (Header + Month Navigator + Actions)
        content.add(buildTopBar(), BorderLayout.NORTH);

        // 2. Center Panel: KPI Cards + Split (Table & Chart)
        JPanel centerContainer = new JPanel(new BorderLayout(0, 16));
        centerContainer.setOpaque(false);

        centerContainer.add(buildKpiCardsPanel(), BorderLayout.NORTH);
        centerContainer.add(buildWorkspaceSplit(), BorderLayout.CENTER);

        content.add(centerContainer, BorderLayout.CENTER);
        return content;
    }

    private JPanel buildTopBar() {
        JPanel topBar = new JPanel(new BorderLayout(15, 0));
        topBar.setOpaque(false);

        // Left: Title and Month Navigator
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);

        periodTitleLabel = new JLabel("All Recorded Transactions");
        periodTitleLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        periodTitleLabel.setForeground(new Color(15, 23, 42));

        // Month switcher: ◀ [November 2026] ▶
        JPanel monthNavPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        monthNavPanel.setOpaque(false);

        prevMonthBtn = new CustomButton("◀", new Color(241, 245, 249), new Color(30, 41, 59), 6);
        prevMonthBtn.setPreferredSize(new Dimension(32, 28));
        prevMonthBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        prevMonthBtn.addActionListener(e -> {
            selectedMonth = selectedMonth.minusMonths(1);
            currentViewMode = "MONTH";
            loadDashboardData();
        });

        monthDisplayLabel = new JLabel("November 2026");
        monthDisplayLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        monthDisplayLabel.setForeground(new Color(51, 65, 85));

        nextMonthBtn = new CustomButton("▶", new Color(241, 245, 249), new Color(30, 41, 59), 6);
        nextMonthBtn.setPreferredSize(new Dimension(32, 28));
        nextMonthBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nextMonthBtn.addActionListener(e -> {
            selectedMonth = selectedMonth.plusMonths(1);
            currentViewMode = "MONTH";
            loadDashboardData();
        });

        monthNavPanel.add(prevMonthBtn);
        monthNavPanel.add(monthDisplayLabel);
        monthNavPanel.add(nextMonthBtn);

        leftPanel.add(periodTitleLabel);
        leftPanel.add(monthNavPanel);

        // Right: Database status badge + Search + Add Button
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightPanel.setOpaque(false);

        boolean isLive = DBConnection.isLiveDatabase();
        dbStatusBadge = new JLabel(isLive ? "● MySQL Online" : "● Demo Offline Mode");
        dbStatusBadge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        dbStatusBadge.setForeground(isLive ? new Color(22, 101, 52) : new Color(180, 83, 9));
        rightPanel.add(dbStatusBadge);

        // Search Input with visible label
        JLabel searchLbl = new JLabel("Search:");
        searchLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        searchLbl.setForeground(new Color(71, 85, 105));
        rightPanel.add(searchLbl);

        searchField = new JTextField(12);
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        searchField.setPreferredSize(new Dimension(160, 34));
        searchField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(203, 213, 225), 1, true),
                new EmptyBorder(4, 8, 4, 8)
        ));
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterTableLocally();
            }
        });
        rightPanel.add(searchField);

        // High-Contrast Bold "+ New Transaction" Button
        addTransactionBtn = new CustomButton("+ New Transaction", new Color(37, 99, 235), Color.WHITE, 8);
        addTransactionBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        addTransactionBtn.setPreferredSize(new Dimension(170, 36));
        addTransactionBtn.setHoverBg(new Color(29, 78, 216));
        addTransactionBtn.addActionListener(e -> openAddDialog());
        rightPanel.add(addTransactionBtn);

        topBar.add(leftPanel, BorderLayout.WEST);
        topBar.add(rightPanel, BorderLayout.EAST);
        return topBar;
    }

    private JPanel buildKpiCardsPanel() {
        JPanel cardsGrid = new JPanel(new GridLayout(1, 4, 14, 0));
        cardsGrid.setOpaque(false);

        balanceValueLabel = new JLabel("$0.00");
        incomeValueLabel = new JLabel("$0.00");
        expenseValueLabel = new JLabel("$0.00");
        upcomingValueLabel = new JLabel("$0.00");

        cardsGrid.add(createKpiCard("Net Period Balance", balanceValueLabel, "Real-time calculation", new Color(37, 99, 235)));
        cardsGrid.add(createKpiCard("Total Income", incomeValueLabel, "Earnings & Inflows", new Color(16, 185, 129)));
        cardsGrid.add(createKpiCard("Total Expenses", expenseValueLabel, "Outflows & Spending", new Color(239, 68, 68)));
        cardsGrid.add(createKpiCard("Next Month (Planned)", upcomingValueLabel, "Upcoming commitments", new Color(139, 92, 246)));

        return cardsGrid;
    }

    private JPanel createKpiCard(String title, JLabel valueLabel, String subtext, Color accent) {
        JPanel card = new JPanel(new BorderLayout(6, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(14, 18, 14, 18)
        ));

        JLabel titleLbl = new JLabel(title.toUpperCase());
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        titleLbl.setForeground(new Color(100, 116, 139));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        valueLabel.setForeground(accent);

        JLabel subLbl = new JLabel(subtext);
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        subLbl.setForeground(new Color(148, 163, 184));

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        card.add(subLbl, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildWorkspaceSplit() {
        JPanel splitPanel = new JPanel(new BorderLayout(14, 0));
        splitPanel.setOpaque(false);

        // Left Table Workspace
        JPanel tableContainer = new JPanel(new BorderLayout(0, 10));
        tableContainer.setOpaque(false);

        // Table Action Header (Category filter + Delete)
        JPanel tableHeaderActions = new JPanel(new BorderLayout());
        tableHeaderActions.setOpaque(false);

        JPanel filterLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterLeft.setOpaque(false);

        JLabel filterLbl = new JLabel("Category Filter:");
        filterLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        filterLbl.setForeground(new Color(71, 85, 105));
        categoryFilterCombo = new JComboBox<>(new String[]{"All Categories"});
        categoryFilterCombo.setPreferredSize(new Dimension(160, 30));
        categoryFilterCombo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        categoryFilterCombo.addActionListener(e -> filterTableLocally());

        filterLeft.add(filterLbl);
        filterLeft.add(categoryFilterCombo);

        deleteButton = new CustomButton("✕ Delete Record", new Color(254, 226, 226), new Color(185, 28, 28), 6);
        deleteButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        deleteButton.setPreferredSize(new Dimension(135, 32));
        deleteButton.setEnabled(false);
        deleteButton.addActionListener(e -> deleteSelectedTransaction());

        tableHeaderActions.add(filterLeft, BorderLayout.WEST);
        tableHeaderActions.add(deleteButton, BorderLayout.EAST);

        tableContainer.add(tableHeaderActions, BorderLayout.NORTH);
        tableContainer.add(buildTableComponent(), BorderLayout.CENTER);

        // Right Chart Panel
        chartPanel = new CategoryChartPanel();

        splitPanel.add(tableContainer, BorderLayout.CENTER);
        splitPanel.add(chartPanel, BorderLayout.EAST);
        return splitPanel;
    }

    private JScrollPane buildTableComponent() {
        tableModel = new TransactionTableModel();
        transactionTable = new JTable(tableModel);
        transactionTable.setRowHeight(36);
        transactionTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        transactionTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        transactionTable.getTableHeader().setBackground(new Color(226, 232, 240));
        transactionTable.getTableHeader().setForeground(new Color(30, 41, 59));
        transactionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        transactionTable.setGridColor(new Color(241, 245, 249));

        // Column widths - expanded so no text has ellipsis
        transactionTable.getColumnModel().getColumn(0).setMaxWidth(50);   // ID
        transactionTable.getColumnModel().getColumn(1).setPreferredWidth(105); // Date (e.g. 2026-11-16)
        transactionTable.getColumnModel().getColumn(2).setPreferredWidth(85);  // Type
        transactionTable.getColumnModel().getColumn(3).setPreferredWidth(140); // Category
        transactionTable.getColumnModel().getColumn(4).setPreferredWidth(230); // Description
        transactionTable.getColumnModel().getColumn(5).setPreferredWidth(125); // Amount
        transactionTable.getColumnModel().getColumn(6).setPreferredWidth(130); // Status Badge

        // Custom Renderer for Amount
        transactionTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.RIGHT);
                Transaction tx = tableModel.getTransactionAt(row);
                if (tx != null && !isSelected) {
                    if (tx.getType() == TransactionType.INCOME) {
                        c.setForeground(new Color(22, 101, 52));
                    } else {
                        c.setForeground(new Color(185, 28, 28));
                    }
                    setFont(new Font("Segoe UI", Font.BOLD, 13));
                }
                return c;
            }
        });

        // Custom Renderer for Status (Pill badge style)
        transactionTable.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                Transaction tx = tableModel.getTransactionAt(row);
                if (tx != null && !isSelected) {
                    if (tx.isUpcoming()) {
                        c.setForeground(new Color(124, 58, 237)); // Violet badge
                    } else {
                        c.setForeground(new Color(100, 116, 139)); // Slate
                    }
                    setFont(new Font("Segoe UI", Font.BOLD, 12));
                }
                return c;
            }
        });

        // Selection listener to enable/disable Delete button
        transactionTable.getSelectionModel().addListSelectionListener(e -> {
            deleteButton.setEnabled(transactionTable.getSelectedRow() != -1);
        });

        JScrollPane scrollPane = new JScrollPane(transactionTable);
        scrollPane.setBorder(new LineBorder(new Color(226, 232, 240), 1, true));
        scrollPane.getViewport().setBackground(Color.WHITE);
        return scrollPane;
    }

    /**
     * Loads and filters transactions based on active view mode (Month, Upcoming, All, Past)
     */
    public void loadDashboardData() {
        int userId = currentUser.getId();

        // 1. Fetch data according to currentViewMode
        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("MMMM yyyy");
        monthDisplayLabel.setText(selectedMonth.format(monthFmt));

        double periodIncome = 0.0;
        double periodExpense = 0.0;

        if ("ALL".equals(currentViewMode)) {
            activeDataSet = financeService.getUserTransactions(userId);
            periodTitleLabel.setText("All Recorded Transactions");
            periodIncome = financeService.calculateTotalByType(userId, TransactionType.INCOME);
            periodExpense = financeService.calculateTotalByType(userId, TransactionType.EXPENSE);
        } else if ("UPCOMING".equals(currentViewMode)) {
            activeDataSet = financeService.getUpcomingTransactions(userId);
            periodTitleLabel.setText("Next Month & Planned Expenses");
            periodIncome = financeService.calculateUpcomingIncome(userId);
            periodExpense = financeService.calculateUpcomingExpenses(userId);
        } else if ("PAST".equals(currentViewMode)) {
            activeDataSet = financeService.getPastTransactions(userId);
            periodTitleLabel.setText("Past Historical Records");
            periodIncome = financeService.calculateTotalByType(userId, TransactionType.INCOME);
            periodExpense = financeService.calculateTotalByType(userId, TransactionType.EXPENSE);
        } else {
            // Specific selected month
            activeDataSet = financeService.getTransactionsByMonth(userId, selectedMonth.getYear(), selectedMonth.getMonthValue());
            periodTitleLabel.setText("Overview for " + selectedMonth.format(monthFmt));
            periodIncome = financeService.calculateMonthIncome(userId, selectedMonth.getYear(), selectedMonth.getMonthValue());
            periodExpense = financeService.calculateMonthExpense(userId, selectedMonth.getYear(), selectedMonth.getMonthValue());
        }

        // 2. Metric KPI Cards
        double netBalance = periodIncome - periodExpense;
        balanceValueLabel.setText(CurrencyFormatter.formatCurrency(netBalance));
        incomeValueLabel.setText(CurrencyFormatter.formatCurrency(periodIncome));
        expenseValueLabel.setText(CurrencyFormatter.formatCurrency(periodExpense));

        double upcomingTotal = financeService.calculateUpcomingExpenses(userId);
        upcomingValueLabel.setText(CurrencyFormatter.formatCurrency(upcomingTotal));

        // 3. Populate Category Dropdown
        updateCategoryDropdown(activeDataSet);

        // 4. Update Table & Visual Chart
        filterTableLocally();
        updateVisualChart();

        deleteButton.setEnabled(false);
    }

    private void updateVisualChart() {
        int userId = currentUser.getId();
        if ("MONTH".equals(currentViewMode) || "THIS_MONTH".equals(currentViewMode)) {
            chartPanel.updateData(financeService.getCategoryBreakdown(userId, selectedMonth.getYear(), selectedMonth.getMonthValue()));
        } else {
            chartPanel.updateData(financeService.getCategoryBreakdown(userId, null, null));
        }
    }

    private void updateCategoryDropdown(List<Transaction> transactions) {
        String currentSelection = (String) categoryFilterCombo.getSelectedItem();
        categoryFilterCombo.removeAllItems();
        categoryFilterCombo.addItem("All Categories");

        for (Transaction tx : transactions) {
            String cat = tx.getCategory();
            boolean exists = false;
            for (int i = 0; i < categoryFilterCombo.getItemCount(); i++) {
                if (categoryFilterCombo.getItemAt(i).equalsIgnoreCase(cat)) {
                    exists = true;
                    break;
                }
            }
            if (!exists) {
                categoryFilterCombo.addItem(cat);
            }
        }

        if (currentSelection != null) {
            categoryFilterCombo.setSelectedItem(currentSelection);
        }
    }

    private void filterTableLocally() {
        String query = searchField.getText().trim().toLowerCase();
        String selectedCat = (String) categoryFilterCombo.getSelectedItem();

        List<Transaction> filtered = activeDataSet.stream().filter(tx -> {
            boolean matchesCat = (selectedCat == null || "All Categories".equals(selectedCat) ||
                    tx.getCategory().equalsIgnoreCase(selectedCat));

            boolean matchesQuery = query.isEmpty() ||
                    tx.getCategory().toLowerCase().contains(query) ||
                    tx.getDescription().toLowerCase().contains(query) ||
                    String.valueOf(tx.getAmount()).contains(query);

            return matchesCat && matchesQuery;
        }).collect(Collectors.toList());

        tableModel.setTransactions(filtered);
    }

    private void openAddDialog() {
        TransactionDialog dialog = new TransactionDialog(this, currentUser.getId(), financeService, this::loadDashboardData);
        dialog.setVisible(true);
    }

    private void deleteSelectedTransaction() {
        int selectedRow = transactionTable.getSelectedRow();
        if (selectedRow == -1) return;

        Transaction tx = tableModel.getTransactionAt(selectedRow);
        if (tx == null) return;

        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete " + tx.getType() + " record of " + CurrencyFormatter.formatCurrency(tx.getAmount()) +
                        " (" + tx.getCategory() + ")?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            boolean ok = financeService.deleteTransaction(tx.getId(), currentUser.getId());
            if (ok) {
                loadDashboardData();
                JOptionPane.showMessageDialog(this, "Transaction record deleted.", "Deleted", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to delete record.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void performLogout() {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to sign out?", "Sign Out", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            userService.logout();
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
            this.dispose();
        }
    }

    private String getInitials(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) return "U";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
        return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
    }
}
