package com.household.ui;

import com.household.dao.BillDAO;
import com.household.dao.ExpenseDAO;
import com.household.model.Bill;
import com.household.service.ReminderService;
import com.household.service.ReminderService.Alert;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.YearMonth;
import java.util.List;

public class DashboardPanel extends JPanel implements Refreshable {

    private final ExpenseDAO expenseDao = new ExpenseDAO();
    private final BillDAO billDao = new BillDAO();
    private final ReminderService reminders = new ReminderService();

    private final JLabel spentValue = new JLabel("-");
    private final JLabel unpaidValue = new JLabel("-");
    private final JLabel dueSoonValue = new JLabel("-");
    private final JLabel overdueValue = new JLabel("-");

    private final DefaultTableModel upcomingModel = new DefaultTableModel(
            new Object[]{"Bill", "Amount", "Due Date", "Days Left"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel categoryModel = new DefaultTableModel(
            new Object[]{"Category", "Spent"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };

    public DashboardPanel() {
        super(new BorderLayout(0, 16));
        setBackground(UiStyle.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel cards = new JPanel(new GridLayout(1, 4, 16, 0));
        cards.setOpaque(false);
        cards.add(UiStyle.statCard("Spent this month", spentValue, UiStyle.PRIMARY));
        cards.add(UiStyle.statCard("Unpaid bills", unpaidValue, UiStyle.WARNING));
        cards.add(UiStyle.statCard("Due in next 7 days", dueSoonValue, UiStyle.MUTED));
        cards.add(UiStyle.statCard("Overdue bills", overdueValue, UiStyle.DANGER));

        JTable upcoming = new JTable(upcomingModel);
        JTable categories = new JTable(categoryModel);
        UiStyle.styleTable(upcoming);
        UiStyle.styleTable(categories);

        JPanel middle = new JPanel(new GridLayout(1, 2, 16, 0));
        middle.setOpaque(false);
        middle.add(section("Upcoming bills (next 30 days)", new JScrollPane(upcoming)));
        middle.add(section("This month by category", new JScrollPane(categories)));

        add(cards, BorderLayout.NORTH);
        add(middle, BorderLayout.CENTER);
        refresh();
    }

    private JPanel section(String title, JComponent body) {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(UiStyle.CARD);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel l = new JLabel(title);
        l.setFont(new Font("SansSerif", Font.BOLD, 15));
        p.add(l, BorderLayout.NORTH);
        p.add(body, BorderLayout.CENTER);
        return p;
    }

    @Override
    public void refresh() {
        YearMonth ym = YearMonth.now();

        spentValue.setText(UiStyle.money(expenseDao.totalByMonth(ym)));

        List<Bill> unpaid = billDao.findAll().stream().filter(b -> !b.paid()).toList();
        unpaidValue.setText(UiStyle.money(unpaid.stream().mapToDouble(Bill::amount).sum()));

        List<Alert> alerts = reminders.getAlerts();
        overdueValue.setText(String.valueOf(alerts.stream().filter(a -> a.daysLeft() < 0).count()));

        long dueSoon = unpaid.stream()
                .filter(b -> ReminderService.daysLeft(b) >= 0 && ReminderService.daysLeft(b) <= 7)
                .count();
        dueSoonValue.setText(String.valueOf(dueSoon));

        upcomingModel.setRowCount(0);
        for (Bill b : unpaid) {
            long d = ReminderService.daysLeft(b);
            if (d <= 30) {
                upcomingModel.addRow(new Object[]{b.name(), UiStyle.money(b.amount()), b.dueDate(), d});
            }
        }

        categoryModel.setRowCount(0);
        expenseDao.categoryTotals(ym)
                .forEach((cat, total) -> categoryModel.addRow(new Object[]{cat, UiStyle.money(total)}));
    }
}
