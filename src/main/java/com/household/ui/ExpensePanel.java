package com.household.ui;

import com.household.dao.ExpenseDAO;
import com.household.model.Expense;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExpensePanel extends JPanel implements Refreshable {

    private final ExpenseDAO dao = new ExpenseDAO();
    private final Runnable onChange;
    private YearMonth month = YearMonth.now();
    private List<Expense> rows = new ArrayList<>();

    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Date", "Title", "Category", "Amount", "Note"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
    private final JLabel totalLabel = new JLabel();

    public ExpensePanel(Runnable onChange) {
        super(new BorderLayout(0, 12));
        this.onChange = onChange;
        setBackground(UiStyle.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        monthLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        monthLabel.setPreferredSize(new Dimension(220, 30));
        JButton prev = UiStyle.button("<", UiStyle.MUTED);
        JButton next = UiStyle.button(">", UiStyle.MUTED);
        prev.addActionListener(e -> { month = month.minusMonths(1); refresh(); });
        next.addActionListener(e -> { month = month.plusMonths(1); refresh(); });

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        nav.setOpaque(false);
        nav.add(prev);
        nav.add(monthLabel);
        nav.add(next);

        JButton btnAdd = UiStyle.button("+ Add", UiStyle.SUCCESS);
        JButton btnEdit = UiStyle.button("Edit", UiStyle.PRIMARY);
        JButton btnDelete = UiStyle.button("Delete", UiStyle.DANGER);
        btnAdd.addActionListener(e -> addExpense());
        btnEdit.addActionListener(e -> editExpense());
        btnDelete.addActionListener(e -> deleteExpense());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        actions.add(btnAdd);
        actions.add(btnEdit);
        actions.add(btnDelete);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(nav, BorderLayout.WEST);
        top.add(actions, BorderLayout.EAST);

        UiStyle.styleTable(table);
        table.getColumnModel().getColumn(0).setMaxWidth(55);
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editExpense();
            }
        });

        totalLabel.setFont(new Font("SansSerif", Font.BOLD, 15));

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(totalLabel, BorderLayout.SOUTH);
        refresh();
    }

    @Override
    public void refresh() {
        monthLabel.setText(month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " " + month.getYear());
        rows = dao.findByMonth(month);
        model.setRowCount(0);
        double total = 0;
        for (Expense e : rows) {
            model.addRow(new Object[]{
                    e.id(), e.date(), e.title(), e.category(),
                    String.format("%,.2f", e.amount()), e.note()});
            total += e.amount();
        }
        totalLabel.setText("Total spent: " + UiStyle.money(total)
                + "   |   " + rows.size() + " entries");
    }

    private Expense selected() {
        int r = table.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Please select a row first.");
            return null;
        }
        return rows.get(r);
    }

    private void addExpense() {
        Expense e = showForm(this, null);
        if (e != null) UiStyle.guard(this, () -> { dao.insert(e); onChange.run(); });
    }

    private void editExpense() {
        Expense old = selected();
        if (old == null) return;
        Expense updated = showForm(this, old);
        if (updated != null) UiStyle.guard(this, () -> { dao.update(updated); onChange.run(); });
    }

    private void deleteExpense() {
        Expense e = selected();
        if (e == null) return;
        int ok = JOptionPane.showConfirmDialog(this,
                "Delete expense \"" + e.title() + "\"?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            UiStyle.guard(this, () -> { dao.delete(e.id()); onChange.run(); });
        }
    }

    private static Expense showForm(Component parent, Expense ex) {
        JTextField title = new JTextField(ex == null ? "" : ex.title(), 20);
        JComboBox<String> category = new JComboBox<>(UiStyle.CATEGORIES);
        category.setEditable(true);
        if (ex != null) category.setSelectedItem(ex.category());
        JTextField amount = new JTextField(ex == null ? "" : String.valueOf(ex.amount()));
        JTextField date = new JTextField(ex == null ? LocalDate.now().toString() : ex.date().toString());
        JTextField note = new JTextField(ex == null ? "" : ex.note());

        JPanel form = UiStyle.formPanel(
                new String[]{"Title", "Category", "Amount", "Date (yyyy-MM-dd)", "Note"},
                new JComponent[]{title, category, amount, date, note});

        while (true) {
            int ok = JOptionPane.showConfirmDialog(parent, form,
                    ex == null ? "Add Expense" : "Edit Expense",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) return null;
            try {
                String t = title.getText().trim();
                if (t.isEmpty()) throw new IllegalArgumentException("Title is required");

                double amt = Double.parseDouble(amount.getText().trim());
                if (amt <= 0) throw new IllegalArgumentException("Amount must be greater than 0");

                LocalDate d = LocalDate.parse(date.getText().trim());

                String cat = String.valueOf(category.getSelectedItem()).trim();
                if (cat.isEmpty()) throw new IllegalArgumentException("Category is required");

                return new Expense(ex == null ? 0 : ex.id(), d, t, cat, amt, note.getText().trim());
            } catch (DateTimeException | IllegalArgumentException err) {
                JOptionPane.showMessageDialog(parent, "Invalid input: " + err.getMessage(),
                        "Check input", JOptionPane.WARNING_MESSAGE);
            }
        }
    }
}
