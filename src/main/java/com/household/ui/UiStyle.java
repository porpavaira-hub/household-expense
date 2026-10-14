package com.household.ui;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import java.awt.*;

public final class UiStyle {

    public static final Color PRIMARY = new Color(37, 99, 235);
    public static final Color SUCCESS = new Color(22, 163, 74);
    public static final Color DANGER  = new Color(220, 38, 38);
    public static final Color WARNING = new Color(234, 88, 12);
    public static final Color MUTED   = new Color(100, 116, 139);
    public static final Color BG      = new Color(243, 246, 251);
    public static final Color CARD    = Color.WHITE;

    public static final String[] CATEGORIES = {
            "Food", "Groceries", "Rent", "Electricity", "Water", "Gas", "Internet",
            "Mobile", "Transport", "Medical", "Education", "Entertainment",
            "Shopping", "Bills", "Other"
    };

    private UiStyle() {}

    public static String money(double value) {
        return String.format("\u20B9 %,.2f", value);
    }

    public static JButton button(String text, Color bg) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? bg.darker() : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setForeground(Color.WHITE);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static void styleTable(JTable t) {
        t.setRowHeight(28);
        t.setFont(new Font("SansSerif", Font.PLAIN, 13));
        t.setShowVerticalLines(false);
        t.setFillsViewportHeight(true);
        t.setSelectionBackground(new Color(219, 234, 254));
        t.setSelectionForeground(Color.BLACK);
        JTableHeader h = t.getTableHeader();
        h.setFont(new Font("SansSerif", Font.BOLD, 13));
        h.setBackground(new Color(241, 245, 249));
    }

    public static JPanel statCard(String title, JLabel value, Color accent) {
        JPanel card = new JPanel(new BorderLayout(0, 6));
        card.setBackground(CARD);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 5, 0, 0, accent),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));
        JLabel t = new JLabel(title);
        t.setForeground(MUTED);
        t.setFont(new Font("SansSerif", Font.PLAIN, 13));
        value.setFont(new Font("SansSerif", Font.BOLD, 22));
        value.setForeground(accent);
        card.add(t, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    public static JPanel formPanel(String[] labels, JComponent[] fields) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        for (int i = 0; i < labels.length; i++) {
            g.gridx = 0; g.gridy = i; g.weightx = 0;
            p.add(new JLabel(labels[i]), g);
            g.gridx = 1; g.weightx = 1;
            p.add(fields[i], g);
        }
        return p;
    }

    public static void guard(Component parent, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(parent,
                    "Something went wrong:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
