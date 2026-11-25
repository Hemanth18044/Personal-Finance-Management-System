package com.finance.view;

import com.finance.model.Transaction;
import com.finance.model.TransactionType;
import com.finance.util.CurrencyFormatter;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom Swing TableModel implementing data binding for Transactions in a JTable.
 * Supports web-style status indicators for past/settled vs next-month planned expenses.
 */
public class TransactionTableModel extends AbstractTableModel {

    private final String[] columnNames = {"ID", "Date", "Type", "Category", "Description", "Amount", "Status"};
    private List<Transaction> transactions;

    public TransactionTableModel() {
        this.transactions = new ArrayList<>();
    }

    public TransactionTableModel(List<Transaction> transactions) {
        this.transactions = transactions != null ? new ArrayList<>(transactions) : new ArrayList<>();
    }

    public void setTransactions(List<Transaction> transactions) {
        this.transactions = transactions != null ? new ArrayList<>(transactions) : new ArrayList<>();
        fireTableDataChanged();
    }

    public Transaction getTransactionAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < transactions.size()) {
            return transactions.get(rowIndex);
        }
        return null;
    }

    public void addTransaction(Transaction tx) {
        if (tx != null) {
            transactions.add(0, tx);
            fireTableRowsInserted(0, 0);
        }
    }

    public void removeTransactionAt(int rowIndex) {
        if (rowIndex >= 0 && rowIndex < transactions.size()) {
            transactions.remove(rowIndex);
            fireTableRowsDeleted(rowIndex, rowIndex);
        }
    }

    @Override
    public int getRowCount() {
        return transactions.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
            case 0: return Integer.class;
            case 1: return String.class;
            case 2: return TransactionType.class;
            case 3: return String.class;
            case 4: return String.class;
            case 5: return String.class;
            case 6: return String.class;
            default: return Object.class;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Transaction tx = transactions.get(rowIndex);
        switch (columnIndex) {
            case 0:
                return tx.getId();
            case 1:
                return CurrencyFormatter.formatDate(tx.getTransactionDate());
            case 2:
                return tx.getType();
            case 3:
                return tx.getCategory();
            case 4:
                return tx.getDescription();
            case 5:
                String prefix = tx.getType() == TransactionType.INCOME ? "+ " : "- ";
                return prefix + CurrencyFormatter.formatCurrency(tx.getAmount());
            case 6:
                return tx.getStatus();
            default:
                return null;
        }
    }
}
