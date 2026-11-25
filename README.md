# FinanceOS — Personal Finance & Budgeting Platform

A modern desktop application built in **Core Java**, **Java Swing & AWT**, and **JDBC with MySQL** adhering to a clean Model-View-Service-DAO architecture. Features a modern website/SaaS-style dashboard, multi-month navigation, planned expenses forecasting, and visual spending breakdown charts.

---

## 🌟 Key Features

1. **Modern SaaS Website-Style Interface**:
   - **Deep Slate Sidebar (`#0f172a`)**: Brand identity, quick navigation tabs, and user profile avatar card.
   - **4 Real-Time KPI Cards**: Net Period Balance, Total Income, Total Expenses, and Next Month (Planned).
   - **Live Search & Filter**: Instant table search as you type, plus category filter dropdown.
   - **Category Spending Breakdown Chart**: Custom-painted Java 2D graphical progress bars showing percentages and dollar amounts.

2. **Multi-Month & Historical Navigation**:
   - **Month Switcher**: Navigate backwards and forwards across past, current, and future calendar months (`◀ [Month Year] ▶`).
   - Monthly balance, income, and expense calculations dynamically update on the fly.

3. **Next Month's Planned Expenses & Budget Forecasting**:
   - Schedule upcoming commitments for next month (rent, insurance, holidays, subscriptions).
   - Future dates are tagged with a **"Planned / Upcoming"** status badge in the table.
   - Dedicated KPI card tracks total planned liabilities to help prevent overspending.

4. **Robust Layered Architecture & Database**:
   - **PreparedStatements**: SQL injection prevention and precompiled queries.
   - **SessionManager**: Thread-safe Singleton session management.
   - **JTable Data Binding**: Custom `TransactionTableModel` with custom cell renderers.
   - **Dual Mode (Live MySQL + Offline Demo)**: Runs seamlessly with live MySQL or in-memory demo mode when database is offline.

---

## 🚀 How to Run

### 1-Click Launch (Windows)
Double-click [run.bat](file:///c:/Users/heman/OneDrive/Desktop/Java1hem/run.bat).

### Command Line
```powershell
# Compile
javac -d target/classes -sourcepath src/main/java (Get-ChildItem -Recurse -Filter "*.java" src/main/java).FullName
Copy-Item src/main/resources/db.properties target/classes/db.properties

# Run
java -cp target/classes com.finance.Main
```

### Automated Verification Test Suite
```powershell
java -ea -cp target/classes com.finance.QuickVerificationTest
```

---

## 🔑 Default Credentials
- **Username**: `john_doe`
- **Password**: `password123`
