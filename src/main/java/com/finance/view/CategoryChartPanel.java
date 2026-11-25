package com.finance.view;

import com.finance.util.CurrencyFormatter;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.Map;

/**
 * Modern visual breakdown component rendering website-style spending progress bars.
 * Drawn natively using Java 2D Graphics.
 */
public class CategoryChartPanel extends JPanel {

    private Map<String, Double> categoryData;
    private double totalExpense = 0.0;

    private static final Color[] PALETTE = {
            new Color(59, 130, 246),  // Modern Blue
            new Color(16, 185, 129),  // Emerald Green
            new Color(245, 158, 11),  // Amber
            new Color(236, 72, 153),  // Pink
            new Color(139, 92, 246),  // Violet
            new Color(14, 165, 233),  // Sky Blue
            new Color(239, 68, 68),   // Red
            new Color(100, 116, 139)  // Slate
    };

    public CategoryChartPanel() {
        setBackground(Color.WHITE);
        setBorder(new CompoundBorder(
                new LineBorder(new Color(226, 232, 240), 1, true),
                new EmptyBorder(16, 20, 16, 20)
        ));
        setPreferredSize(new Dimension(320, 260));
    }

    public void updateData(Map<String, Double> data) {
        this.categoryData = data;
        this.totalExpense = 0.0;
        if (data != null) {
            for (double val : data.values()) {
                totalExpense += val;
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int width = getWidth() - 40;
        int y = 28;

        // Header Title
        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2.setColor(new Color(30, 41, 59));
        g2.drawString("Spending by Category", 20, y);

        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.setColor(new Color(100, 116, 139));
        String totalStr = "Total: " + CurrencyFormatter.formatCurrency(totalExpense);
        int totalWidth = g2.getFontMetrics().stringWidth(totalStr);
        g2.drawString(totalStr, getWidth() - 20 - totalWidth, y);

        y += 24;

        if (categoryData == null || categoryData.isEmpty() || totalExpense == 0.0) {
            g2.setFont(new Font("Segoe UI", Font.ITALIC, 13));
            g2.setColor(new Color(148, 163, 184));
            g2.drawString("No expense data recorded for this view.", 20, y + 40);
            g2.dispose();
            return;
        }

        int index = 0;
        int maxItems = 6;
        for (Map.Entry<String, Double> entry : categoryData.entrySet()) {
            if (index >= maxItems) break;

            String category = entry.getKey();
            double amount = entry.getValue();
            double ratio = amount / totalExpense;
            int percentage = (int) Math.round(ratio * 100);

            Color barColor = PALETTE[index % PALETTE.length];

            // 1. Text row: Category name (Left) & Amount + Percentage (Right)
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
            g2.setColor(new Color(51, 65, 85));
            g2.drawString(category, 20, y);

            g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            g2.setColor(new Color(100, 116, 139));
            String statStr = CurrencyFormatter.formatCurrency(amount) + " (" + percentage + "%)";
            int statWidth = g2.getFontMetrics().stringWidth(statStr);
            g2.drawString(statStr, getWidth() - 20 - statWidth, y);

            y += 8;

            // 2. Background track bar
            int barHeight = 8;
            g2.setColor(new Color(241, 245, 249));
            g2.fillRoundRect(20, y, width, barHeight, 6, 6);

            // 3. Filled percentage bar
            int filledWidth = (int) Math.max(8, width * ratio);
            g2.setColor(barColor);
            g2.fillRoundRect(20, y, filledWidth, barHeight, 6, 6);

            y += 24;
            index++;
        }

        g2.dispose();
    }
}
