package com.finance.web;

import com.finance.model.BudgetAlert;
import com.finance.model.Transaction;
import com.finance.model.TransactionType;
import com.finance.service.FinanceService;
import com.finance.util.CurrencyFormatter;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;

/**
 * Built-in Lightweight HTTP Web Server providing a modern SaaS web interface
 * at http://localhost:8080 with real-time Category Budgeting and 80%/100% Alert System.
 */
public class WebServer {

    private static final int PORT = 8080;
    private static HttpServer server;
    private static final FinanceService financeService = new FinanceService();

    public static synchronized void start() {
        if (server != null) return;
        try {
            server = HttpServer.create(new InetSocketAddress(PORT), 0);
            server.createContext("/", new DashboardHtmlHandler());
            server.createContext("/api/add", new AddTransactionHandler());
            server.createContext("/api/delete", new DeleteTransactionHandler());
            server.createContext("/api/budget/set", new SetBudgetHandler());
            server.createContext("/api/export-csv", new ExportCsvHandler());
            server.setExecutor(null);
            server.start();
            System.out.println("=======================================================");
            System.out.println("🌐 [Web Server] Modern Web App live at: http://localhost:" + PORT);
            System.out.println("=======================================================");
        } catch (IOException e) {
            System.err.println("[Web Server] Could not start on port " + PORT + ": " + e.getMessage());
        }
    }

    public static synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    static class DashboardHtmlHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            int userId = 1; // John Doe default
            String query = exchange.getRequestURI().getQuery();
            YearMonth ym = YearMonth.now();

            String alertType = null;
            String alertMsg = null;

            if (query != null) {
                for (String part : query.split("&")) {
                    if (part.startsWith("month=")) {
                        try {
                            ym = YearMonth.parse(part.substring(6));
                        } catch (Exception ignored) {}
                    } else if (part.startsWith("alert=")) {
                        alertType = part.substring(6);
                    } else if (part.startsWith("msg=")) {
                        try {
                            alertMsg = URLDecoder.decode(part.substring(4), StandardCharsets.UTF_8);
                        } catch (Exception ignored) {}
                    }
                }
            }

            List<Transaction> transactions = financeService.getTransactionsByMonth(userId, ym.getYear(), ym.getMonthValue());
            double income = financeService.calculateMonthIncome(userId, ym.getYear(), ym.getMonthValue());
            double expense = financeService.calculateMonthExpense(userId, ym.getYear(), ym.getMonthValue());
            double balance = income - expense;
            double upcoming = financeService.calculateUpcomingExpenses(userId);
            Map<String, Double> breakdown = financeService.getCategoryBreakdown(userId, ym.getYear(), ym.getMonthValue());
            Map<String, Double> allBudgets = financeService.getAllBudgets(userId);

            double savingsRate = income > 0 ? Math.max(0, Math.min(100, Math.round(((income - expense) / income) * 100))) : 0;

            String prevMonth = ym.minusMonths(1).toString();
            String nextMonth = ym.plusMonths(1).toString();
            String currentDisplay = ym.getMonth().name().substring(0, 1).toUpperCase() + 
                                   ym.getMonth().name().substring(1).toLowerCase() + " " + ym.getYear();

            // Build dynamic table rows with modern styling & data attributes
            StringBuilder rows = new StringBuilder();
            if (transactions.isEmpty()) {
                rows.append("<tr class='empty-row'><td colspan='8' style='text-align:center; padding:36px; color:#94a3b8;'>")
                    .append("<div style='font-size:32px; margin-bottom:8px;'>📂</div>")
                    .append("<div style='font-size:14px; font-weight:600; color:#475569;'>No transactions recorded for this period</div>")
                    .append("<div style='font-size:12px; margin-top:4px;'>Click <strong>+ New Transaction</strong> above to log your first record</div>")
                    .append("</td></tr>");
            } else {
                for (Transaction tx : transactions) {
                    boolean isInc = tx.getType() == TransactionType.INCOME;
                    String amtColor = isInc ? "#059669" : "#e11d48";
                    String prefix = isInc ? "+ " : "- ";
                    String typeBadge = isInc 
                        ? "<span class='badge-pill pill-inc'>↑ Income</span>" 
                        : "<span class='badge-pill pill-exp'>↓ Expense</span>";

                    String statusBadge = tx.isUpcoming()
                        ? "<span class='badge-pill pill-upcoming'>⏳ Planned / Upcoming</span>"
                        : "<span class='badge-pill pill-settled'>✓ Settled</span>";

                    String categoryIcon = getCategoryIcon(tx.getCategory());

                    rows.append("<tr class='tx-row' data-type='").append(tx.getType()).append("' data-status='").append(tx.isUpcoming() ? "UPCOMING" : "SETTLED").append("'>")
                        .append("<td class='tx-id'>#").append(tx.getId()).append("</td>")
                        .append("<td class='tx-date'>").append(CurrencyFormatter.formatDate(tx.getTransactionDate())).append("</td>")
                        .append("<td>").append(typeBadge).append("</td>")
                        .append("<td class='tx-category'><span class='cat-icon'>").append(categoryIcon).append("</span> <strong>").append(escapeHtml(tx.getCategory())).append("</strong></td>")
                        .append("<td class='tx-desc'>").append(escapeHtml(tx.getDescription().isEmpty() ? "—" : tx.getDescription())).append("</td>")
                        .append("<td class='tx-amount' style='color:").append(amtColor).append(";'>").append(prefix).append(CurrencyFormatter.formatCurrency(tx.getAmount())).append("</td>")
                        .append("<td>").append(statusBadge).append("</td>")
                        .append("<td class='tx-action'>")
                        .append("<form method='POST' action='/api/delete' style='display:inline;' onsubmit='return confirm(\"Are you sure you want to delete this ").append(tx.getType()).append(" of ").append(CurrencyFormatter.formatCurrency(tx.getAmount())).append("?\");'>")
                        .append("<input type='hidden' name='id' value='").append(tx.getId()).append("'>")
                        .append("<input type='hidden' name='month' value='").append(ym.toString()).append("'>")
                        .append("<button type='submit' class='btn-trash' title='Delete record'>🗑</button>")
                        .append("</form>")
                        .append("</td>")
                        .append("</tr>");
                }
            }

            // Build Category Budget Trackers with 80% Warning & 100% Alert visual bars
            StringBuilder budgetCards = new StringBuilder();
            StringBuilder budgetJson = new StringBuilder("{");
            StringBuilder spentJson = new StringBuilder("{");

            int bCount = 0;
            for (Map.Entry<String, Double> bEntry : allBudgets.entrySet()) {
                String cat = bEntry.getKey();
                double limit = bEntry.getValue();
                double spent = financeService.getCategoryMonthSpend(userId, cat, ym.getYear(), ym.getMonthValue());
                int pct = (int) Math.round((spent / limit) * 100.0);

                if (bCount > 0) {
                    budgetJson.append(",");
                    spentJson.append(",");
                }
                budgetJson.append("\"").append(cat.toLowerCase()).append("\":").append(limit);
                spentJson.append("\"").append(cat.toLowerCase()).append("\":").append(spent);
                bCount++;

                String barColor;
                String statusPill;
                if (pct >= 100) {
                    barColor = "#e11d48"; // Red
                    statusPill = "<span class='badge-pill' style='background:#ffe4e6; color:#9f1239; border:1px solid #fecdd3;'>🚨 " + pct + "% Exceeded</span>";
                } else if (pct >= 80) {
                    barColor = "#f59e0b"; // Yellow/Amber
                    statusPill = "<span class='badge-pill' style='background:#fef3c7; color:#92400e; border:1px solid #fde68a;'>⚠️ " + pct + "% Warning</span>";
                } else {
                    barColor = "#10b981"; // Green
                    statusPill = "<span class='badge-pill' style='background:#d1fae5; color:#065f46;'>✓ " + pct + "% Normal</span>";
                }

                budgetCards.append("<div class='cat-bar-item'>")
                    .append("<div class='cat-bar-header'>")
                    .append("<span class='cat-name'>").append(getCategoryIcon(cat)).append(" ").append(escapeHtml(cat)).append("</span>")
                    .append("<div>").append(statusPill).append("</div>")
                    .append("</div>")
                    .append("<div style='display:flex; justify-content:space-between; font-size:11px; color:#64748b; margin-bottom:5px; font-weight:600;'>")
                    .append("<span>Spent: ").append(CurrencyFormatter.formatCurrency(spent)).append("</span>")
                    .append("<span>Limit: ").append(CurrencyFormatter.formatCurrency(limit)).append("</span>")
                    .append("</div>")
                    .append("<div class='progress-track'>")
                    .append("<div class='progress-fill' style='background:").append(barColor).append("; width:").append(Math.min(100, Math.max(3, pct))).append("%;'></div>")
                    .append("</div></div>");
            }
            budgetJson.append("}");
            spentJson.append("}");

            // Category breakdown bars
            StringBuilder bars = new StringBuilder();
            if (breakdown.isEmpty() || expense == 0) {
                bars.append("<div style='text-align:center; padding:18px 12px; color:#94a3b8; font-size:12px;'>No expenses recorded for this month.</div>");
            } else {
                String[] colors = {"#3b82f6", "#10b981", "#f59e0b", "#ec4899", "#8b5cf6", "#06b6d4", "#f43f5e"};
                int cIdx = 0;
                for (Map.Entry<String, Double> e : breakdown.entrySet()) {
                    int pct = (int) Math.round((e.getValue() / expense) * 100);
                    String color = colors[cIdx % colors.length];
                    cIdx++;
                    bars.append("<div class='cat-bar-item'>")
                        .append("<div class='cat-bar-header'>")
                        .append("<span class='cat-name'>").append(getCategoryIcon(e.getKey())).append(" ").append(escapeHtml(e.getKey())).append("</span>")
                        .append("<span class='cat-amount'>").append(CurrencyFormatter.formatCurrency(e.getValue())).append(" <small>(").append(pct).append("%)</small></span>")
                        .append("</div>")
                        .append("<div class='progress-track'>")
                        .append("<div class='progress-fill' style='background:").append(color).append("; width:").append(Math.min(100, Math.max(4, pct))).append("%;'></div>")
                        .append("</div></div>");
                }
            }

            // Build Floating Notification Banner if alert is active
            StringBuilder alertHtml = new StringBuilder();
            if (alertType != null && alertMsg != null) {
                boolean isDanger = "danger".equalsIgnoreCase(alertType);
                String toastBg = isDanger ? "#fee2e2" : "#fef3c7";
                String toastBorder = isDanger ? "#f87171" : "#fcd34d";
                String toastColor = isDanger ? "#991b1b" : "#92400e";
                String toastIcon = isDanger ? "🚨" : "⚠️";

                alertHtml.append("<div class='toast-alert' style='background:").append(toastBg)
                         .append("; border: 1px solid ").append(toastBorder)
                         .append("; color: ").append(toastColor).append(";'>")
                         .append("<div style='display:flex; align-items:center; gap:12px;'>")
                         .append("<span style='font-size:20px;'>").append(toastIcon).append("</span>")
                         .append("<div>")
                         .append("<div style='font-weight:800; font-size:13px;'>").append(isDanger ? "BUDGET EXCEEDED ALERT" : "BUDGET THRESHOLD WARNING").append("</div>")
                         .append("<div style='font-size:12px; margin-top:2px;'>").append(escapeHtml(alertMsg)).append("</div>")
                         .append("</div>")
                         .append("</div>")
                         .append("<button onclick='this.parentElement.remove()' style='background:none; border:none; font-size:18px; cursor:pointer; color:inherit;'>&times;</button>")
                         .append("</div>");
            }

            // Build Full Next-Generation SaaS HTML5 Page
            String html = "<!DOCTYPE html>\n" +
                "<html lang='en'>\n" +
                "<head>\n" +
                "  <meta charset='UTF-8'>\n" +
                "  <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n" +
                "  <title>FinanceOS — Smart Wealth & Budgeting Dashboard</title>\n" +
                "  <link rel='preconnect' href='https://fonts.googleapis.com'>\n" +
                "  <link rel='preconnect' href='https://fonts.gstatic.com' crossorigin>\n" +
                "  <link href='https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&family=JetBrains+Mono:wght@500&display=swap' rel='stylesheet'>\n" +
                "  <style>\n" +
                "    :root {\n" +
                "      --bg-body: #f8fafc;\n" +
                "      --bg-surface: #ffffff;\n" +
                "      --sidebar-bg: #0b0f19;\n" +
                "      --sidebar-border: rgba(255, 255, 255, 0.08);\n" +
                "      --border-color: #e2e8f0;\n" +
                "      --text-main: #0f172a;\n" +
                "      --text-muted: #64748b;\n" +
                "      --text-sub: #94a3b8;\n" +
                "      --primary: #2563eb;\n" +
                "      --primary-hover: #1d4ed8;\n" +
                "      --success: #059669;\n" +
                "      --danger: #e11d48;\n" +
                "      --warning: #d97706;\n" +
                "      --purple: #7c3aed;\n" +
                "      --shadow-sm: 0 1px 2px 0 rgba(0, 0, 0, 0.05);\n" +
                "      --shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.07), 0 2px 4px -2px rgba(0, 0, 0, 0.05);\n" +
                "      --shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.08), 0 4px 6px -4px rgba(0, 0, 0, 0.04);\n" +
                "      --radius-sm: 8px;\n" +
                "      --radius-md: 12px;\n" +
                "      --radius-lg: 16px;\n" +
                "    }\n" +
                "    * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Plus Jakarta Sans', -apple-system, BlinkMacSystemFont, sans-serif; }\n" +
                "    body { display: flex; background: var(--bg-body); color: var(--text-main); height: 100vh; overflow: hidden; }\n" +
                "\n" +
                "    /* === FLOATING ALERT TOAST === */\n" +
                "    .toast-alert {\n" +
                "      position: fixed;\n" +
                "      top: 20px;\n" +
                "      right: 36px;\n" +
                "      z-index: 9999;\n" +
                "      display: flex;\n" +
                "      align-items: center;\n" +
                "      justify-content: space-between;\n" +
                "      gap: 16px;\n" +
                "      padding: 14px 20px;\n" +
                "      border-radius: var(--radius-md);\n" +
                "      box-shadow: 0 10px 25px -5px rgba(0, 0, 0, 0.15);\n" +
                "      animation: slideDown 0.3s cubic-bezier(0.16, 1, 0.3, 1);\n" +
                "      max-width: 480px;\n" +
                "    }\n" +
                "    @keyframes slideDown { from { transform: translateY(-40px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }\n" +
                "\n" +
                "    /* === SIDEBAR === */\n" +
                "    .sidebar {\n" +
                "      width: 260px;\n" +
                "      background: var(--sidebar-bg);\n" +
                "      color: #fff;\n" +
                "      padding: 24px 18px;\n" +
                "      display: flex;\n" +
                "      flex-direction: column;\n" +
                "      justify-content: space-between;\n" +
                "      border-right: 1px solid var(--sidebar-border);\n" +
                "      flex-shrink: 0;\n" +
                "    }\n" +
                "    .brand {\n" +
                "      display: flex;\n" +
                "      align-items: center;\n" +
                "      gap: 12px;\n" +
                "      padding: 4px 8px 24px 8px;\n" +
                "      border-bottom: 1px solid var(--sidebar-border);\n" +
                "      margin-bottom: 20px;\n" +
                "    }\n" +
                "    .brand-icon {\n" +
                "      width: 38px;\n" +
                "      height: 38px;\n" +
                "      background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%);\n" +
                "      border-radius: 10px;\n" +
                "      display: flex;\n" +
                "      align-items: center;\n" +
                "      justify-content: center;\n" +
                "      font-size: 20px;\n" +
                "      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.4);\n" +
                "    }\n" +
                "    .brand-title { font-size: 18px; font-weight: 800; letter-spacing: -0.5px; color: #fff; }\n" +
                "    .brand-badge { font-size: 10px; font-weight: 700; color: #38bdf8; text-transform: uppercase; letter-spacing: 0.8px; }\n" +
                "    .nav-label { font-size: 11px; font-weight: 700; color: #475569; text-transform: uppercase; letter-spacing: 0.8px; padding: 10px 12px 6px; }\n" +
                "    .nav-link {\n" +
                "      display: flex;\n" +
                "      align-items: center;\n" +
                "      gap: 12px;\n" +
                "      padding: 10px 14px;\n" +
                "      margin-bottom: 4px;\n" +
                "      border-radius: var(--radius-sm);\n" +
                "      text-decoration: none;\n" +
                "      color: #94a3b8;\n" +
                "      font-size: 13px;\n" +
                "      font-weight: 600;\n" +
                "      transition: all 0.2s ease;\n" +
                "    }\n" +
                "    .nav-link:hover, .nav-link.active {\n" +
                "      background: rgba(255, 255, 255, 0.08);\n" +
                "      color: #fff;\n" +
                "      transform: translateX(2px);\n" +
                "    }\n" +
                "    .nav-link.active {\n" +
                "      background: linear-gradient(90deg, rgba(37, 99, 235, 0.2) 0%, rgba(37, 99, 235, 0.05) 100%);\n" +
                "      color: #60a5fa;\n" +
                "      border-left: 3px solid #3b82f6;\n" +
                "    }\n" +
                "    .user-footer {\n" +
                "      background: rgba(255, 255, 255, 0.04);\n" +
                "      border: 1px solid var(--sidebar-border);\n" +
                "      border-radius: var(--radius-md);\n" +
                "      padding: 12px;\n" +
                "      display: flex;\n" +
                "      align-items: center;\n" +
                "      gap: 10px;\n" +
                "    }\n" +
                "    .user-avatar {\n" +
                "      width: 38px;\n" +
                "      height: 38px;\n" +
                "      background: linear-gradient(135deg, #6366f1, #3b82f6);\n" +
                "      border-radius: 50%;\n" +
                "      display: flex;\n" +
                "      align-items: center;\n" +
                "      justify-content: center;\n" +
                "      font-weight: 700;\n" +
                "      font-size: 14px;\n" +
                "      color: #fff;\n" +
                "    }\n" +
                "\n" +
                "    /* === MAIN VIEWPORT === */\n" +
                "    .viewport { flex: 1; display: flex; flex-direction: column; overflow: hidden; }\n" +
                "    .topbar {\n" +
                "      background: var(--bg-surface);\n" +
                "      border-bottom: 1px solid var(--border-color);\n" +
                "      padding: 16px 36px;\n" +
                "      display: flex;\n" +
                "      justify-content: space-between;\n" +
                "      align-items: center;\n" +
                "    }\n" +
                "    .period-navigator {\n" +
                "      display: flex;\n" +
                "      align-items: center;\n" +
                "      gap: 12px;\n" +
                "      background: #f1f5f9;\n" +
                "      padding: 4px;\n" +
                "      border-radius: 10px;\n" +
                "    }\n" +
                "    .period-navigator a {\n" +
                "      text-decoration: none;\n" +
                "      background: #fff;\n" +
                "      border: 1px solid #e2e8f0;\n" +
                "      color: var(--text-main);\n" +
                "      width: 28px;\n" +
                "      height: 28px;\n" +
                "      display: flex;\n" +
                "      align-items: center;\n" +
                "      justify-content: center;\n" +
                "      border-radius: 8px;\n" +
                "      font-size: 12px;\n" +
                "      font-weight: bold;\n" +
                "      transition: 0.15s;\n" +
                "    }\n" +
                "    .period-navigator a:hover { background: #e2e8f0; }\n" +
                "    .period-label { font-size: 14px; font-weight: 700; color: #1e293b; padding: 0 8px; }\n" +
                "    .action-group { display: flex; align-items: center; gap: 10px; }\n" +
                "    .btn {\n" +
                "      display: inline-flex;\n" +
                "      align-items: center;\n" +
                "      gap: 8px;\n" +
                "      padding: 10px 18px;\n" +
                "      border-radius: 9px;\n" +
                "      font-size: 13px;\n" +
                "      font-weight: 600;\n" +
                "      cursor: pointer;\n" +
                "      border: none;\n" +
                "      transition: all 0.2s;\n" +
                "      text-decoration: none;\n" +
                "    }\n" +
                "    .btn-primary {\n" +
                "      background: linear-gradient(135deg, #2563eb, #1d4ed8);\n" +
                "      color: #fff;\n" +
                "      box-shadow: 0 4px 12px rgba(37, 99, 235, 0.25);\n" +
                "    }\n" +
                "    .btn-primary:hover { background: #1e40af; transform: translateY(-1px); }\n" +
                "    .btn-budget {\n" +
                "      background: #fdf4ff;\n" +
                "      border: 1px solid #f0abfc;\n" +
                "      color: #a21caf;\n" +
                "    }\n" +
                "    .btn-budget:hover { background: #fae8ff; }\n" +
                "    .btn-outline {\n" +
                "      background: #fff;\n" +
                "      border: 1px solid var(--border-color);\n" +
                "      color: var(--text-main);\n" +
                "    }\n" +
                "    .btn-outline:hover { background: #f8fafc; border-color: #cbd5e1; }\n" +
                "\n" +
                "    /* === DASHBOARD CONTENT SCROLL === */\n" +
                "    .content { flex: 1; padding: 28px 36px; overflow-y: auto; }\n" +
                "\n" +
                "    /* === 4 METRIC KPI CARDS === */\n" +
                "    .kpi-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 18px; margin-bottom: 24px; }\n" +
                "    .kpi-card {\n" +
                "      background: var(--bg-surface);\n" +
                "      border: 1px solid var(--border-color);\n" +
                "      border-radius: var(--radius-md);\n" +
                "      padding: 20px 22px;\n" +
                "      position: relative;\n" +
                "      overflow: hidden;\n" +
                "      box-shadow: var(--shadow-sm);\n" +
                "      transition: transform 0.2s, box-shadow 0.2s;\n" +
                "    }\n" +
                "    .kpi-card:hover { transform: translateY(-3px); box-shadow: var(--shadow-md); }\n" +
                "    .kpi-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }\n" +
                "    .kpi-title { font-size: 12px; font-weight: 700; color: var(--text-muted); text-transform: uppercase; letter-spacing: 0.5px; }\n" +
                "    .kpi-icon-box {\n" +
                "      width: 32px;\n" +
                "      height: 32px;\n" +
                "      border-radius: 8px;\n" +
                "      display: flex;\n" +
                "      align-items: center;\n" +
                "      justify-content: center;\n" +
                "      font-size: 14px;\n" +
                "    }\n" +
                "    .kpi-val { font-size: 26px; font-weight: 800; letter-spacing: -0.5px; margin-bottom: 6px; }\n" +
                "    .kpi-meta { font-size: 12px; color: var(--text-sub); display: flex; align-items: center; gap: 6px; }\n" +
                "\n" +
                "    /* === WORKSPACE SPLIT (Table + Category Analytics & Budgets) === */\n" +
                "    .workspace-split { display: grid; grid-template-columns: 2fr 1fr; gap: 24px; }\n" +
                "    .panel-card {\n" +
                "      background: var(--bg-surface);\n" +
                "      border: 1px solid var(--border-color);\n" +
                "      border-radius: var(--radius-md);\n" +
                "      padding: 24px;\n" +
                "      box-shadow: var(--shadow-sm);\n" +
                "      margin-bottom: 24px;\n" +
                "    }\n" +
                "    .panel-header {\n" +
                "      display: flex;\n" +
                "      justify-content: space-between;\n" +
                "      align-items: center;\n" +
                "      margin-bottom: 18px;\n" +
                "      flex-wrap: wrap;\n" +
                "      gap: 12px;\n" +
                "    }\n" +
                "    .panel-title { font-size: 16px; font-weight: 700; color: var(--text-main); }\n" +
                "\n" +
                "    /* Table Controls */\n" +
                "    .table-controls { display: flex; align-items: center; gap: 10px; width: 100%; margin-bottom: 16px; }\n" +
                "    .search-box {\n" +
                "      flex: 1;\n" +
                "      display: flex;\n" +
                "      align-items: center;\n" +
                "      background: #f8fafc;\n" +
                "      border: 1px solid var(--border-color);\n" +
                "      border-radius: 8px;\n" +
                "      padding: 8px 14px;\n" +
                "      gap: 8px;\n" +
                "    }\n" +
                "    .search-box input {\n" +
                "      border: none;\n" +
                "      outline: none;\n" +
                "      background: transparent;\n" +
                "      width: 100%;\n" +
                "      font-size: 13px;\n" +
                "    }\n" +
                "    .filter-tabs { display: flex; gap: 6px; background: #f1f5f9; padding: 3px; border-radius: 8px; }\n" +
                "    .tab-btn {\n" +
                "      border: none;\n" +
                "      background: transparent;\n" +
                "      padding: 6px 12px;\n" +
                "      font-size: 12px;\n" +
                "      font-weight: 600;\n" +
                "      color: var(--text-muted);\n" +
                "      border-radius: 6px;\n" +
                "      cursor: pointer;\n" +
                "      transition: 0.15s;\n" +
                "    }\n" +
                "    .tab-btn.active {\n" +
                "      background: #fff;\n" +
                "      color: var(--text-main);\n" +
                "      box-shadow: 0 1px 3px rgba(0,0,0,0.06);\n" +
                "    }\n" +
                "\n" +
                "    /* Data Table */\n" +
                "    .table-responsive { width: 100%; overflow-x: auto; border-radius: 8px; border: 1px solid #f1f5f9; }\n" +
                "    table { width: 100%; border-collapse: collapse; text-align: left; font-size: 13px; }\n" +
                "    th {\n" +
                "      background: #f8fafc;\n" +
                "      color: #64748b;\n" +
                "      font-weight: 700;\n" +
                "      font-size: 11px;\n" +
                "      text-transform: uppercase;\n" +
                "      letter-spacing: 0.5px;\n" +
                "      padding: 12px 14px;\n" +
                "      border-bottom: 1px solid var(--border-color);\n" +
                "    }\n" +
                "    td { padding: 14px; border-bottom: 1px solid #f1f5f9; vertical-align: middle; }\n" +
                "    tr.tx-row:hover { background: #f8fafc; }\n" +
                "    .tx-id { color: #94a3b8; font-family: 'JetBrains Mono', monospace; font-size: 12px; }\n" +
                "    .tx-date { font-weight: 600; color: #475569; white-space: nowrap; }\n" +
                "    .cat-icon { margin-right: 6px; }\n" +
                "    .tx-desc { color: #64748b; max-width: 220px; text-overflow: ellipsis; overflow: hidden; white-space: nowrap; }\n" +
                "    .tx-amount { font-weight: 700; font-family: 'JetBrains Mono', monospace; font-size: 14px; white-space: nowrap; }\n" +
                "\n" +
                "    /* Badge Pills */\n" +
                "    .badge-pill { display: inline-flex; align-items: center; gap: 4px; padding: 4px 10px; border-radius: 20px; font-size: 11px; font-weight: 700; }\n" +
                "    .pill-inc { background: #d1fae5; color: #065f46; }\n" +
                "    .pill-exp { background: #ffe4e6; color: #9f1239; }\n" +
                "    .pill-settled { background: #f1f5f9; color: #475569; }\n" +
                "    .pill-upcoming { background: #f3e8ff; color: #6b21a8; border: 1px dashed #c084fc; }\n" +
                "    .btn-trash {\n" +
                "      background: transparent;\n" +
                "      border: none;\n" +
                "      cursor: pointer;\n" +
                "      font-size: 15px;\n" +
                "      padding: 6px 8px;\n" +
                "      border-radius: 6px;\n" +
                "      transition: 0.15s;\n" +
                "      opacity: 0.6;\n" +
                "    }\n" +
                "    .btn-trash:hover { opacity: 1; background: #fee2e2; transform: scale(1.1); }\n" +
                "\n" +
                "    /* Category Bars */\n" +
                "    .cat-bar-item { margin-bottom: 16px; }\n" +
                "    .cat-bar-header { display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 6px; font-weight: 600; }\n" +
                "    .cat-name { color: #334155; display: flex; align-items: center; gap: 6px; }\n" +
                "    .cat-amount { color: #64748b; font-family: 'JetBrains Mono', monospace; }\n" +
                "    .progress-track { background: #f1f5f9; height: 9px; border-radius: 6px; overflow: hidden; }\n" +
                "    .progress-fill { height: 100%; border-radius: 6px; transition: width 0.4s ease; }\n" +
                "\n" +
                "    /* Modal Popups */\n" +
                "    .modal-overlay {\n" +
                "      display: none;\n" +
                "      position: fixed;\n" +
                "      inset: 0;\n" +
                "      background: rgba(15, 23, 42, 0.5);\n" +
                "      backdrop-filter: blur(4px);\n" +
                "      justify-content: center;\n" +
                "      align-items: center;\n" +
                "      z-index: 1000;\n" +
                "      animation: fadeIn 0.2s ease;\n" +
                "    }\n" +
                "    .modal-card {\n" +
                "      background: #fff;\n" +
                "      border-radius: var(--radius-lg);\n" +
                "      width: 460px;\n" +
                "      padding: 30px;\n" +
                "      box-shadow: var(--shadow-lg);\n" +
                "    }\n" +
                "    .modal-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }\n" +
                "    .modal-title { font-size: 18px; font-weight: 800; color: #0f172a; }\n" +
                "    .modal-close { background: none; border: none; font-size: 20px; cursor: pointer; color: #94a3b8; }\n" +
                "    .form-group { margin-bottom: 16px; }\n" +
                "    .form-group label { display: block; font-size: 12px; font-weight: 700; color: #475569; margin-bottom: 6px; }\n" +
                "    .form-group input, .form-group select {\n" +
                "      width: 100%;\n" +
                "      padding: 10px 14px;\n" +
                "      border: 1px solid var(--border-color);\n" +
                "      border-radius: 8px;\n" +
                "      font-size: 13px;\n" +
                "      outline: none;\n" +
                "      transition: 0.15s;\n" +
                "    }\n" +
                "    .form-group input:focus, .form-group select:focus { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(37,99,235,0.1); }\n" +
                "    .preset-chips { display: flex; gap: 6px; margin-top: 6px; flex-wrap: wrap; }\n" +
                "    .chip-btn {\n" +
                "      background: #f1f5f9;\n" +
                "      border: 1px solid #e2e8f0;\n" +
                "      padding: 5px 10px;\n" +
                "      font-size: 11px;\n" +
                "      font-weight: 600;\n" +
                "      border-radius: 6px;\n" +
                "      cursor: pointer;\n" +
                "      transition: 0.15s;\n" +
                "    }\n" +
                "    .chip-btn:hover { background: #e2e8f0; }\n" +
                "    .future-notice {\n" +
                "      font-size: 11px;\n" +
                "      font-weight: 700;\n" +
                "      margin-top: 6px;\n" +
                "      color: #7c3aed;\n" +
                "      display: none;\n" +
                "    }\n" +
                "    .live-budget-warning {\n" +
                "      margin-top: 6px;\n" +
                "      padding: 8px 12px;\n" +
                "      border-radius: 6px;\n" +
                "      font-size: 12px;\n" +
                "      font-weight: 700;\n" +
                "      display: none;\n" +
                "    }\n" +
                "    @keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "\n" +
                "  " + alertHtml.toString() + "\n" +
                "\n" +
                "  <!-- SIDEBAR NAVIGATION -->\n" +
                "  <aside class='sidebar'>\n" +
                "    <div>\n" +
                "      <div class='brand'>\n" +
                "        <div class='brand-icon'>◈</div>\n" +
                "        <div>\n" +
                "          <div class='brand-title'>FinanceOS</div>\n" +
                "          <div class='brand-badge'>SaaS Wealth Suite</div>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "      <div class='nav-label'>Dashboards</div>\n" +
                "      <a href='/?month=" + YearMonth.now().toString() + "' class='nav-link active'><span>📊</span> Overview</a>\n" +
                "      <a href='/?month=" + YearMonth.now().toString() + "' class='nav-link'><span>📅</span> This Month</a>\n" +
                "      <a href='/?month=" + YearMonth.now().plusMonths(1).toString() + "' class='nav-link'><span>⏳</span> Next Month (Planned)</a>\n" +
                "      <a href='/?month=" + YearMonth.now().minusMonths(1).toString() + "' class='nav-link'><span>📜</span> Past Archive</a>\n" +
                "      <div class='nav-label' style='margin-top:14px;'>Budgeting</div>\n" +
                "      <a href='javascript:void(0)' onclick='openBudgetModal()' class='nav-link'><span>🎯</span> Set Category Limits</a>\n" +
                "      <a href='/api/export-csv' class='nav-link'><span>📥</span> Export CSV</a>\n" +
                "    </div>\n" +
                "\n" +
                "    <div class='user-footer'>\n" +
                "      <div class='user-avatar'>JD</div>\n" +
                "      <div style='flex:1;'>\n" +
                "        <div style='font-size:13px; font-weight:700;'>John Doe</div>\n" +
                "        <div style='font-size:11px; color:#94a3b8;'>@john_doe</div>\n" +
                "      </div>\n" +
                "      <span title='Active Online Session' style='font-size:12px;'>🟢</span>\n" +
                "    </div>\n" +
                "  </aside>\n" +
                "\n" +
                "  <!-- MAIN VIEWPORT -->\n" +
                "  <div class='viewport'>\n" +
                "    <!-- TOP CONTROL BAR -->\n" +
                "    <header class='topbar'>\n" +
                "      <div class='period-navigator'>\n" +
                "        <a href='/?month=" + prevMonth + "' title='Previous Month'>◀</a>\n" +
                "        <span class='period-label'>" + currentDisplay + "</span>\n" +
                "        <a href='/?month=" + nextMonth + "' title='Next Month'>▶</a>\n" +
                "      </div>\n" +
                "      <div class='action-group'>\n" +
                "        <button class='btn btn-budget' onclick='openBudgetModal()'><span>🎯</span> Set Monthly Budget</button>\n" +
                "        <button class='btn btn-primary' onclick='openModal()'><span>+</span> New Transaction</button>\n" +
                "      </div>\n" +
                "    </header>\n" +
                "\n" +
                "    <!-- MAIN DASHBOARD CONTENT -->\n" +
                "    <main class='content'>\n" +
                "      <!-- 4 METRIC KPI CARDS -->\n" +
                "      <section class='kpi-grid'>\n" +
                "        <div class='kpi-card'>\n" +
                "          <div class='kpi-header'>\n" +
                "            <span class='kpi-title'>Net Period Balance</span>\n" +
                "            <span class='kpi-icon-box' style='background:#eff6ff; color:#2563eb;'>💰</span>\n" +
                "          </div>\n" +
                "          <div class='kpi-val' style='color:" + (balance >= 0 ? "#2563eb" : "#e11d48") + ";'>" + CurrencyFormatter.formatCurrency(balance) + "</div>\n" +
                "          <div class='kpi-meta'>Real-time cashflow balance</div>\n" +
                "        </div>\n" +
                "\n" +
                "        <div class='kpi-card'>\n" +
                "          <div class='kpi-header'>\n" +
                "            <span class='kpi-title'>Total Income</span>\n" +
                "            <span class='kpi-icon-box' style='background:#ecfdf5; color:#059669;'>📈</span>\n" +
                "          </div>\n" +
                "          <div class='kpi-val' style='color:#059669;'>" + CurrencyFormatter.formatCurrency(income) + "</div>\n" +
                "          <div class='kpi-meta'>Earnings & Inflows</div>\n" +
                "        </div>\n" +
                "\n" +
                "        <div class='kpi-card'>\n" +
                "          <div class='kpi-header'>\n" +
                "            <span class='kpi-title'>Total Expenses</span>\n" +
                "            <span class='kpi-icon-box' style='background:#fff1f2; color:#e11d48;'>📉</span>\n" +
                "          </div>\n" +
                "          <div class='kpi-val' style='color:#e11d48;'>" + CurrencyFormatter.formatCurrency(expense) + "</div>\n" +
                "          <div class='kpi-meta'>Spending & Outflows</div>\n" +
                "        </div>\n" +
                "\n" +
                "        <div class='kpi-card'>\n" +
                "          <div class='kpi-header'>\n" +
                "            <span class='kpi-title'>Next Month (Planned)</span>\n" +
                "            <span class='kpi-icon-box' style='background:#f5f3ff; color:#7c3aed;'>⏳</span>\n" +
                "          </div>\n" +
                "          <div class='kpi-val' style='color:#7c3aed;'>" + CurrencyFormatter.formatCurrency(upcoming) + "</div>\n" +
                "          <div class='kpi-meta'>Upcoming liabilities</div>\n" +
                "        </div>\n" +
                "      </section>\n" +
                "\n" +
                "      <!-- SPLIT VIEW: Table & Category Budgets & Analytics -->\n" +
                "      <section class='workspace-split'>\n" +
                "        <!-- TABLE CARD -->\n" +
                "        <div class='panel-card'>\n" +
                "          <div class='panel-header'>\n" +
                "            <div class='panel-title'>Transaction History (" + transactions.size() + ")</div>\n" +
                "          </div>\n" +
                "          <div class='table-controls'>\n" +
                "            <div class='search-box'>\n" +
                "              <span>🔍</span>\n" +
                "              <input type='text' id='searchInput' placeholder='Search by category, description or amount...' onkeyup='filterTable()'>\n" +
                "            </div>\n" +
                "            <div class='filter-tabs'>\n" +
                "              <button class='tab-btn active' onclick='setFilterTab(this, \"ALL\")'>All</button>\n" +
                "              <button class='tab-btn' onclick='setFilterTab(this, \"INCOME\")'>Income</button>\n" +
                "              <button class='tab-btn' onclick='setFilterTab(this, \"EXPENSE\")'>Expenses</button>\n" +
                "              <button class='tab-btn' onclick='setFilterTab(this, \"UPCOMING\")'>Planned</button>\n" +
                "            </div>\n" +
                "          </div>\n" +
                "          <div class='table-responsive'>\n" +
                "            <table id='txTable'>\n" +
                "              <thead>\n" +
                "                <tr>\n" +
                "                  <th>ID</th>\n" +
                "                  <th>Date</th>\n" +
                "                  <th>Type</th>\n" +
                "                  <th>Category</th>\n" +
                "                  <th>Description</th>\n" +
                "                  <th>Amount</th>\n" +
                "                  <th>Status</th>\n" +
                "                  <th>Action</th>\n" +
                "                </tr>\n" +
                "              </thead>\n" +
                "              <tbody>\n" +
                "                " + rows.toString() + "\n" +
                "              </tbody>\n" +
                "            </table>\n" +
                "          </div>\n" +
                "        </div>\n" +
                "\n" +
                "        <!-- RIGHT COLUMN: Budgets Tracker + Analytics -->\n" +
                "        <div>\n" +
                "          <!-- REAL-TIME CATEGORY BUDGETS TRACKER -->\n" +
                "          <div class='panel-card'>\n" +
                "            <div class='panel-header'>\n" +
                "              <div class='panel-title'>Category Budget Alerts</div>\n" +
                "              <button onclick='openBudgetModal()' style='background:none; border:none; font-size:12px; font-weight:700; color:var(--primary); cursor:pointer;'>+ Set Limit</button>\n" +
                "            </div>\n" +
                "            <div>\n" +
                "              " + budgetCards.toString() + "\n" +
                "            </div>\n" +
                "            <div style='font-size:11px; color:#64748b; margin-top:8px;'>⚠️ Yellow warning at 80% • 🚨 Red alert at 100% limit</div>\n" +
                "          </div>\n" +
                "\n" +
                "          <!-- SPENDING ANALYTICS -->\n" +
                "          <div class='panel-card'>\n" +
                "            <div class='panel-header'>\n" +
                "              <div class='panel-title'>Monthly Breakdown</div>\n" +
                "            </div>\n" +
                "            <div>\n" +
                "              " + bars.toString() + "\n" +
                "            </div>\n" +
                "          </div>\n" +
                "        </div>\n" +
                "      </section>\n" +
                "    </main>\n" +
                "  </div>\n" +
                "\n" +
                "  <!-- ADD TRANSACTION MODAL -->\n" +
                "  <div class='modal-overlay' id='modalOverlay' onclick='if(event.target===this) closeModal()'>\n" +
                "    <div class='modal-card'>\n" +
                "      <div class='modal-head'>\n" +
                "        <div class='modal-title'>Record Transaction</div>\n" +
                "        <button class='modal-close' onclick='closeModal()'>&times;</button>\n" +
                "      </div>\n" +
                "      <form method='POST' action='/api/add'>\n" +
                "        <input type='hidden' name='currentMonth' value='" + ym.toString() + "'>\n" +
                "        <div class='form-group'>\n" +
                "          <label>Transaction Type</label>\n" +
                "          <select name='type' id='typeSelect' onchange='handleTypeChange()'>\n" +
                "            <option value='EXPENSE'>Expense (Outflow)</option>\n" +
                "            <option value='INCOME'>Income (Inflow)</option>\n" +
                "          </select>\n" +
                "        </div>\n" +
                "        <div class='form-group'>\n" +
                "          <label>Category</label>\n" +
                "          <input type='text' id='categoryInput' name='category' required placeholder='e.g. Food, Groceries, Housing & Rent' oninput='checkLiveBudget()'>\n" +
                "          <div class='preset-chips' id='categoryChips'>\n" +
                "            <button type='button' class='chip-btn' onclick='setCategory(\"Food\")'>🍔 Food</button>\n" +
                "            <button type='button' class='chip-btn' onclick='setCategory(\"Groceries\")'>🛒 Groceries</button>\n" +
                "            <button type='button' class='chip-btn' onclick='setCategory(\"Housing & Rent\")'>🏠 Rent</button>\n" +
                "            <button type='button' class='chip-btn' onclick='setCategory(\"Utilities & Bills\")'>⚡ Bills</button>\n" +
                "            <button type='button' class='chip-btn' onclick='setCategory(\"Dining Out\")'>🍽 Dining</button>\n" +
                "          </div>\n" +
                "        </div>\n" +
                "        <div class='form-group'>\n" +
                "          <label>Amount ($ USD)</label>\n" +
                "          <input type='number' step='0.01' id='amountInput' name='amount' required placeholder='0.00' oninput='checkLiveBudget()'>\n" +
                "          <div id='liveBudgetNotice' class='live-budget-warning'></div>\n" +
                "        </div>\n" +
                "        <div class='form-group'>\n" +
                "          <label>Date (YYYY-MM-DD)</label>\n" +
                "          <input type='date' id='dateInput' name='date' value='" + LocalDate.now().toString() + "' required onchange='checkFutureDate()'>\n" +
                "          <div class='preset-chips'>\n" +
                "            <button type='button' class='chip-btn' onclick='setDate(\"" + LocalDate.now() + "\")'>Today</button>\n" +
                "            <button type='button' class='chip-btn' onclick='setDate(\"" + LocalDate.now().plusMonths(1).withDayOfMonth(1) + "\")'>Next Month 1st (Planned)</button>\n" +
                "            <button type='button' class='chip-btn' onclick='setDate(\"" + LocalDate.now().minusMonths(1).withDayOfMonth(15) + "\")'>Past Month</button>\n" +
                "          </div>\n" +
                "          <div id='futureNotice' class='future-notice'>✦ Future Date: Will be marked as Planned/Upcoming Expense</div>\n" +
                "        </div>\n" +
                "        <div class='form-group'>\n" +
                "          <label>Description (Optional)</label>\n" +
                "          <input type='text' name='description' placeholder='e.g. Weekly supermarket visit or dining'>\n" +
                "        </div>\n" +
                "        <div style='display:flex; justify-content:flex-end; gap:10px; margin-top:24px;'>\n" +
                "          <button type='button' onclick='closeModal()' class='btn btn-outline'>Cancel</button>\n" +
                "          <button type='submit' class='btn btn-primary'>Save Record</button>\n" +
                "        </div>\n" +
                "      </form>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "\n" +
                "  <!-- SET BUDGET MODAL -->\n" +
                "  <div class='modal-overlay' id='budgetModalOverlay' onclick='if(event.target===this) closeBudgetModal()'>\n" +
                "    <div class='modal-card'>\n" +
                "      <div class='modal-head'>\n" +
                "        <div class='modal-title'>🎯 Set Monthly Category Budget</div>\n" +
                "        <button class='modal-close' onclick='closeBudgetModal()'>&times;</button>\n" +
                "      </div>\n" +
                "      <form method='POST' action='/api/budget/set'>\n" +
                "        <input type='hidden' name='currentMonth' value='" + ym.toString() + "'>\n" +
                "        <div class='form-group'>\n" +
                "          <label>Category</label>\n" +
                "          <input type='text' id='budgetCategoryInput' name='category' required placeholder='e.g. Food, Groceries, Dining Out'>\n" +
                "          <div class='preset-chips'>\n" +
                "            <button type='button' class='chip-btn' onclick='setBudgetCat(\"Food\")'>🍔 Food</button>\n" +
                "            <button type='button' class='chip-btn' onclick='setBudgetCat(\"Groceries\")'>🛒 Groceries</button>\n" +
                "            <button type='button' class='chip-btn' onclick='setBudgetCat(\"Dining Out\")'>🍽 Dining Out</button>\n" +
                "            <button type='button' class='chip-btn' onclick='setBudgetCat(\"Shopping\")'>🛍 Shopping</button>\n" +
                "          </div>\n" +
                "        </div>\n" +
                "        <div class='form-group'>\n" +
                "          <label>Monthly Spending Limit ($ USD)</label>\n" +
                "          <input type='number' step='0.01' name='limit' required placeholder='500.00'>\n" +
                "          <div style='font-size:11px; color:#64748b; margin-top:4px;'>Alerts will trigger at 80% (yellow warning) and 100% (red exceeded).</div>\n" +
                "        </div>\n" +
                "        <div style='display:flex; justify-content:flex-end; gap:10px; margin-top:24px;'>\n" +
                "          <button type='button' onclick='closeBudgetModal()' class='btn btn-outline'>Cancel</button>\n" +
                "          <button type='submit' class='btn btn-primary'>Save Budget Limit</button>\n" +
                "        </div>\n" +
                "      </form>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "\n" +
                "  <script>\n" +
                "    const BUDGETS = " + budgetJson.toString() + ";\n" +
                "    const SPENT = " + spentJson.toString() + ";\n" +
                "    let currentFilterType = 'ALL';\n" +
                "\n" +
                "    function openModal() { document.getElementById('modalOverlay').style.display = 'flex'; }\n" +
                "    function closeModal() { document.getElementById('modalOverlay').style.display = 'none'; }\n" +
                "    function openBudgetModal() { document.getElementById('budgetModalOverlay').style.display = 'flex'; }\n" +
                "    function closeBudgetModal() { document.getElementById('budgetModalOverlay').style.display = 'none'; }\n" +
                "    function setDate(val) { document.getElementById('dateInput').value = val; checkFutureDate(); }\n" +
                "    function setCategory(cat) { document.getElementById('categoryInput').value = cat; checkLiveBudget(); }\n" +
                "    function setBudgetCat(cat) { document.getElementById('budgetCategoryInput').value = cat; }\n" +
                "\n" +
                "    function checkLiveBudget() {\n" +
                "      const type = document.getElementById('typeSelect').value;\n" +
                "      const cat = (document.getElementById('categoryInput').value || '').trim().toLowerCase();\n" +
                "      const amt = parseFloat(document.getElementById('amountInput').value || 0);\n" +
                "      const notice = document.getElementById('liveBudgetNotice');\n" +
                "\n" +
                "      if (type !== 'EXPENSE' || !cat || amt <= 0 || !BUDGETS[cat]) {\n" +
                "        notice.style.display = 'none';\n" +
                "        return;\n" +
                "      }\n" +
                "\n" +
                "      const limit = BUDGETS[cat];\n" +
                "      const cur = SPENT[cat] || 0;\n" +
                "      const projected = cur + amt;\n" +
                "      const pct = Math.round((projected / limit) * 100);\n" +
                "\n" +
                "      notice.style.display = 'block';\n" +
                "      if (pct >= 100) {\n" +
                "        notice.style.background = '#ffe4e6';\n" +
                "        notice.style.color = '#9f1239';\n" +
                "        notice.style.border = '1px solid #fecdd3';\n" +
                "        notice.innerHTML = `🚨 <strong>Budget Alert:</strong> This expense will exceed your $${limit.toFixed(2)} budget for ${cat} (Projected: $${projected.toFixed(2)} / ${pct}%)`;\n" +
                "      } else if (pct >= 80) {\n" +
                "        notice.style.background = '#fef3c7';\n" +
                "        notice.style.color = '#92400e';\n" +
                "        notice.style.border = '1px solid #fde68a';\n" +
                "        notice.innerHTML = `⚠️ <strong>Budget Warning:</strong> You will reach ${pct}% of your $${limit.toFixed(2)} budget for ${cat} (Projected: $${projected.toFixed(2)})`;\n" +
                "      } else {\n" +
                "        notice.style.background = '#d1fae5';\n" +
                "        notice.style.color = '#065f46';\n" +
                "        notice.style.border = '1px solid #a7f3d0';\n" +
                "        notice.innerHTML = `✓ Within budget: Projected $${projected.toFixed(2)} of $${limit.toFixed(2)} (${pct}%)`;\n" +
                "      }\n" +
                "    }\n" +
                "\n" +
                "    function checkFutureDate() {\n" +
                "      const val = document.getElementById('dateInput').value;\n" +
                "      const isFuture = new Date(val) > new Date();\n" +
                "      document.getElementById('futureNotice').style.display = isFuture ? 'block' : 'none';\n" +
                "    }\n" +
                "\n" +
                "    function handleTypeChange() {\n" +
                "      const t = document.getElementById('typeSelect').value;\n" +
                "      const chips = document.getElementById('categoryChips');\n" +
                "      if (t === 'INCOME') {\n" +
                "        chips.innerHTML = `<button type='button' class='chip-btn' onclick='setCategory(\"Salary\")'>💼 Salary</button>` +\n" +
                "                          `<button type='button' class='chip-btn' onclick='setCategory(\"Freelance\")'>💻 Freelance</button>` +\n" +
                "                          `<button type='button' class='chip-btn' onclick='setCategory(\"Investments\")'>📈 Investments</button>` +\n" +
                "                          `<button type='button' class='chip-btn' onclick='setCategory(\"Bonus\")'>🎁 Bonus</button>`;\n" +
                "      } else {\n" +
                "        chips.innerHTML = `<button type='button' class='chip-btn' onclick='setCategory(\"Food\")'>🍔 Food</button>` +\n" +
                "                          `<button type='button' class='chip-btn' onclick='setCategory(\"Groceries\")'>🛒 Groceries</button>` +\n" +
                "                          `<button type='button' class='chip-btn' onclick='setCategory(\"Housing & Rent\")'>🏠 Rent</button>` +\n" +
                "                          `<button type='button' class='chip-btn' onclick='setCategory(\"Utilities & Bills\")'>⚡ Bills</button>` +\n" +
                "                          `<button type='button' class='chip-btn' onclick='setCategory(\"Dining Out\")'>🍽 Dining</button>`;\n" +
                "      }\n" +
                "      checkLiveBudget();\n" +
                "    }\n" +
                "\n" +
                "    function setFilterTab(btn, mode) {\n" +
                "      document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));\n" +
                "      btn.classList.add('active');\n" +
                "      currentFilterType = mode;\n" +
                "      filterTable();\n" +
                "    }\n" +
                "\n" +
                "    function filterTable() {\n" +
                "      const query = document.getElementById('searchInput').value.toLowerCase();\n" +
                "      const rows = document.querySelectorAll('#txTable tbody tr.tx-row');\n" +
                "      rows.forEach(r => {\n" +
                "        const type = r.getAttribute('data-type');\n" +
                "        const status = r.getAttribute('data-status');\n" +
                "        const text = r.innerText.toLowerCase();\n" +
                "        let matchesTab = (currentFilterType === 'ALL') ||\n" +
                "                         (currentFilterType === 'INCOME' && type === 'INCOME') ||\n" +
                "                         (currentFilterType === 'EXPENSE' && type === 'EXPENSE') ||\n" +
                "                         (currentFilterType === 'UPCOMING' && status === 'UPCOMING');\n" +
                "        let matchesSearch = text.includes(query);\n" +
                "        r.style.display = (matchesTab && matchesSearch) ? '' : 'none';\n" +
                "      });\n" +
                "    }\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";

            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    static class AddTransactionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseFormData(exchange.getRequestBody());
                String redirectMonth = params.getOrDefault("currentMonth", YearMonth.now().toString());

                try {
                    int userId = 1;
                    double amount = Double.parseDouble(params.getOrDefault("amount", "0"));
                    TransactionType type = TransactionType.fromString(params.getOrDefault("type", "EXPENSE"));
                    String category = params.getOrDefault("category", "General");
                    String description = params.getOrDefault("description", "");
                    String dateStr = params.getOrDefault("date", LocalDate.now().toString());
                    Date date = CurrencyFormatter.parseDate(dateStr);

                    // Add transaction and evaluate 80%/100% budget alerts
                    BudgetAlert alert = financeService.addTransactionWithAlert(userId, amount, type, category, description, date);
                    if (alert != null && alert.hasAlert()) {
                        String alertKind = alert.getLevel() == BudgetAlert.Level.EXCEEDED ? "danger" : "warning";
                        redirectMonth += "&alert=" + alertKind + "&msg=" + URLEncoder.encode(alert.getMessage(), StandardCharsets.UTF_8);
                    }
                } catch (Exception e) {
                    System.err.println("[Web Server] Error adding transaction: " + e.getMessage());
                }

                exchange.getResponseHeaders().set("Location", "/?month=" + redirectMonth);
                exchange.sendResponseHeaders(303, -1);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    static class SetBudgetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseFormData(exchange.getRequestBody());
                String redirectMonth = params.getOrDefault("currentMonth", YearMonth.now().toString());

                try {
                    int userId = 1;
                    String category = params.getOrDefault("category", "");
                    double limit = Double.parseDouble(params.getOrDefault("limit", "0"));

                    if (!category.trim().isEmpty() && limit > 0) {
                        financeService.setBudget(userId, category.trim(), limit);
                    }
                } catch (Exception e) {
                    System.err.println("[Web Server] Error setting budget limit: " + e.getMessage());
                }

                exchange.getResponseHeaders().set("Location", "/?month=" + redirectMonth);
                exchange.sendResponseHeaders(303, -1);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    static class DeleteTransactionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> params = parseFormData(exchange.getRequestBody());
                try {
                    int id = Integer.parseInt(params.getOrDefault("id", "0"));
                    financeService.deleteTransaction(id, 1);
                } catch (Exception e) {
                    System.err.println("[Web Server] Error deleting transaction: " + e.getMessage());
                }

                String redirectMonth = params.getOrDefault("month", YearMonth.now().toString());
                exchange.getResponseHeaders().set("Location", "/?month=" + redirectMonth);
                exchange.sendResponseHeaders(303, -1);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        }
    }

    static class ExportCsvHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            int userId = 1;
            List<Transaction> list = financeService.getUserTransactions(userId);

            StringBuilder csv = new StringBuilder();
            csv.append("ID,Date,Type,Category,Description,Amount,Status\n");
            for (Transaction tx : list) {
                csv.append(tx.getId()).append(",")
                   .append(CurrencyFormatter.formatDate(tx.getTransactionDate())).append(",")
                   .append(tx.getType()).append(",")
                   .append("\"").append(tx.getCategory().replace("\"", "\"\"")).append("\",")
                   .append("\"").append(tx.getDescription().replace("\"", "\"\"")).append("\",")
                   .append(tx.getAmount()).append(",")
                   .append(tx.getStatus()).append("\n");
            }

            byte[] bytes = csv.toString().getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"financeos_transactions.csv\"");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    private static String getCategoryIcon(String category) {
        if (category == null) return "💳";
        String lower = category.toLowerCase();
        if (lower.contains("rent") || lower.contains("house")) return "🏠";
        if (lower.contains("grocer") || lower.contains("food")) return "🛒";
        if (lower.contains("util") || lower.contains("bill") || lower.contains("power")) return "⚡";
        if (lower.contains("din") || lower.contains("restaur")) return "🍽";
        if (lower.contains("salary") || lower.contains("paycheck")) return "💼";
        if (lower.contains("free") || lower.contains("tech")) return "💻";
        if (lower.contains("invest") || lower.contains("div")) return "📈";
        if (lower.contains("travel") || lower.contains("train") || lower.contains("flight")) return "✈️";
        if (lower.contains("shop") || lower.contains("gift")) return "🛍";
        if (lower.contains("sub") || lower.contains("soft")) return "📱";
        return "🏷";
    }

    private static Map<String, String> parseFormData(InputStream is) throws IOException {
        Map<String, String> map = new HashMap<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        String body = sb.toString();
        if (!body.isEmpty()) {
            for (String pair : body.split("&")) {
                String[] parts = pair.split("=", 2);
                if (parts.length == 2) {
                    map.put(URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                            URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
                }
            }
        }
        return map;
    }

    private static String escapeHtml(String str) {
        if (str == null) return "";
        return str.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;");
    }
}
