package com.household.ui;

import com.household.service.ReminderService;
import com.household.service.ReminderService.Alert;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class MainFrame extends JFrame {

    private final ReminderService reminders = new ReminderService();

    private final JTabbedPane tabs = new JTabbedPane();
    private final DashboardPanel dashboard = new DashboardPanel();
    private final ExpensePanel expensePanel;
    private final BillPanel billPanel;

    private final JPanel banner = new JPanel(new BorderLayout());
    private final JLabel bannerText = new JLabel();

    public MainFrame() {
        super("Household Expense & Bill Reminder");

        expensePanel = new ExpensePanel(this::refreshAll);
        billPanel = new BillPanel(this::refreshAll);

        tabs.setFont(new Font("SansSerif", Font.BOLD, 14));
        tabs.addTab("Dashboard", dashboard);
        tabs.addTab("Expenses", expensePanel);
        tabs.addTab("Bills", billPanel);
        tabs.addChangeListener(e -> refreshAll());

        bannerText.setFont(new Font("SansSerif", Font.BOLD, 14));
        JButton viewBills = UiStyle.button("View Bills", UiStyle.WARNING);
        viewBills.addActionListener(e -> tabs.setSelectedComponent(billPanel));
        banner.setBackground(new Color(255, 247, 237));
        banner.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        banner.add(bannerText, BorderLayout.CENTER);
        banner.add(viewBills, BorderLayout.EAST);
        banner.setVisible(false);

        setLayout(new BorderLayout());
        add(banner, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1150, 720);
        setMinimumSize(new Dimension(950, 620));
        setLocationRelativeTo(null);

        refreshAll();

        new Timer(30 * 60 * 1000, e -> updateBanner()).start();
        SwingUtilities.invokeLater(this::showStartupReminders);
    }

    public void refreshAll() {
        dashboard.refresh();
        expensePanel.refresh();
        billPanel.refresh();
        updateBanner();
    }

    private void updateBanner() {
        List<Alert> alerts = reminders.getAlerts();
        long overdue = alerts.stream().filter(a -> a.daysLeft() < 0).count();
        long soon = alerts.size() - overdue;

        banner.setVisible(!alerts.isEmpty());
        if (!alerts.isEmpty()) {
            bannerText.setText("!  " + overdue + " overdue   |   " + soon + " due soon");
            bannerText.setForeground(overdue > 0 ? UiStyle.DANGER : UiStyle.WARNING);
        }
    }

    private void showStartupReminders() {
        List<Alert> alerts = reminders.getAlerts();
        if (alerts.isEmpty()) return;

        StringBuilder sb = new StringBuilder("Bills that need your attention:\n\n");
        for (Alert a : alerts) {
            long d = a.daysLeft();
            String when = d < 0 ? "OVERDUE by " + (-d) + " day(s)"
                        : d == 0 ? "Due TODAY"
                        : "Due in " + d + " day(s)";
            sb.append("- ").append(a.bill().name())
              .append("  |  ").append(UiStyle.money(a.bill().amount()))
              .append("  |  ").append(when).append('\n');
        }
        JOptionPane.showMessageDialog(this, sb.toString(),
                "Bill Reminder", JOptionPane.INFORMATION_MESSAGE);
    }
}
