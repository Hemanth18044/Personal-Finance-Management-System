package com.finance.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Custom-painted JButton with guaranteed vibrant colors and high-contrast text,
 * completely immune to Windows Look and Feel button overrides.
 */
public class CustomButton extends JButton {

    private Color normalBg;
    private Color hoverBg;
    private Color normalFg;
    private Color hoverFg;
    private Color disabledBg = new Color(241, 245, 249);
    private Color disabledFg = new Color(148, 163, 184);
    private int cornerRadius = 8;
    private boolean isHovered = false;

    public CustomButton(String text, Color bg, Color fg) {
        this(text, bg, fg, 8);
    }

    public CustomButton(String text, Color bg, Color fg, int radius) {
        super(text);
        this.normalBg = bg;
        this.hoverBg = calculateHoverColor(bg);
        this.normalFg = fg;
        this.hoverFg = fg;
        this.cornerRadius = radius;

        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setForeground(fg);
        setCursor(new Cursor(Cursor.HAND_CURSOR));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    isHovered = true;
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                isHovered = false;
                repaint();
            }
        });
    }

    public void setColors(Color bg, Color fg) {
        this.normalBg = bg;
        this.hoverBg = calculateHoverColor(bg);
        this.normalFg = fg;
        this.hoverFg = fg;
        repaint();
    }

    public void setHoverBg(Color hoverBg) {
        this.hoverBg = hoverBg;
    }

    private Color calculateHoverColor(Color c) {
        if (c.getRed() < 50 && c.getGreen() < 60 && c.getBlue() < 80) {
            // Dark button -> brighten on hover
            return new Color(c.getRed() + 25, c.getGreen() + 25, c.getBlue() + 35);
        } else {
            // Light/Vibrant button -> darken slightly on hover
            int r = Math.max(0, c.getRed() - 20);
            int g = Math.max(0, c.getGreen() - 20);
            int b = Math.max(0, c.getBlue() - 20);
            return new Color(r, g, b);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 1. Draw Background
        Color bg = !isEnabled() ? disabledBg : (isHovered ? hoverBg : normalBg);
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), cornerRadius, cornerRadius);

        // 2. Draw Text with perfect contrast
        Color fg = !isEnabled() ? disabledFg : (isHovered ? hoverFg : normalFg);
        g2.setColor(fg);
        g2.setFont(getFont());

        FontMetrics fm = g2.getFontMetrics();
        int textWidth = fm.stringWidth(getText());
        int textHeight = fm.getAscent();

        int x;
        if (getHorizontalAlignment() == SwingConstants.LEFT) {
            x = 16;
        } else {
            x = (getWidth() - textWidth) / 2;
        }
        int y = (getHeight() + textHeight) / 2 - 2;

        g2.drawString(getText(), x, y);
        g2.dispose();
    }
}
