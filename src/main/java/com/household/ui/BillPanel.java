package com.household.ui;

import com.household.dao.BillDAO;
import com.household.model.Bill;
import com.household.model.Frequency;
import com.household.service.BillService;
import com.household.service.ReminderService;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BillPanel extends JPanel implements Refreshable {

    private final BillDAO dao = new BillDAO();
    private final BillService service = new BillService();
    private final Runnable onChange;
    private List<Bill> rows = new ArrayList<>();

    private final JComboBox<String> filter = new JComboBox<>(new String[]{"All", "Unpaid", "Paid"});
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"ID", "Bill", "Amount", "Due Date", "Frequency", "Days Left", "Status"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JLabel summary = new JLabel();

    public BillPanel(Runnable onChange) {
        super(new BorderLayout(0, 12));
        this.onChange = onChange;
        setBackground(UiStyle.BG);
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(new JLabel("Show:"));
        left.add(filter);
        filter.addActionListener(e -> refresh());

        JButton btnAdd = UiStyle.button("+ Add", UiStyle.SUCCESS);
        JButton btnEdit = UiStyle.button("Edit", UiStyle.PRIMARY);
        JButton btnPaid = UiStyle.button("Mark Paid", UiStyle.WARNING);
        JButton btnDelete = UiStyle.button("Delete", UiStyle.DANGER);
        btnAdd.addActionListener(e -> addBill());
        btnEdit.addActionListener(e -> editBill());
        btnPaid.addActionListener(e -> markPaid());
        btnDelete.addActionListener(e -> deleteBill());

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(btnAdd);
        right.add(btnEdit);
        right.add(btnPaid);
        right.add(btnDelete);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(left, BorderLayout.WEST);
        top.add(right, BorderLayout.EAST);

        UiStyle.styleTable(table);
        table.setDefaultRenderer(Object.class, new RowColorRenderer());
        table.getColumnModel().getColumn(0).setMaxWidth(55);
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editBill();
            }
        });

        summary.setFont(new Font("SansSerif", Font.BOLD, 15));

        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(summary, BorderLayout.SOUTH);
        refresh();
    }

    @Override
    public void refresh() {
        String f = (String) filter.getSelectedItem();
        rows = dao.findAll().stream()
                .filter(b -> switch (f) {
                    case "Unpaid" -> !b.paid();
                    case "Paid"   -> b.paid();
                    default       -> true;
                })
                .toList();

        model.setRowCount(0);
        double unpaidTotal = 0;
        for (Bill b : rows) {
            long days = ReminderService.daysLeft(b);
            model.addRow(new Object[]{
                    b.id(), b.name(), UiStyle.money(b.amount()), b.dueDate(),
                    b.frequency(), b.paid() ? "-" : String.valueOf(days), statusOf(b, days)});
            if (!b.paid()) unpaidTotal += b.amount();
        }
        summary.setText("Unpaid in view: " + UiStyle.money(unpaidTotal)
                + "   |   " + rows.size() + " bills shown");
    }

    private static String statusOf(Bill b, long days) {
        if (b.paid()) return "PAID";
        if (days < 0) return "OVERDUE";
        if (days <= b.reminderDays()) return "DUE SOON";
        return "UPCOMING";
    }

    private static Color rowColor(Bill b) {
        return switch (statusOf(b, ReminderService.daysLeft(b))) {
            case "PAID"     -> new Color(220, 252, 231);
            case "OVERDUE"  -> new Color(254, 226, 226);
            case "DUE SOON" -> new Color(255, 237, 213);
            default         -> Color.WHITE;
        };
    }

    private class RowColorRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value,
                                                       boolean selected, boolean focused,
                                                       int row, int col) {
            Component c = super.getTableCellRendererComponent(t, value, selected, focused, row, col);
            if (!selected) c.setBackground(rowColor(rows.get(row)));
            return c;
        }
    }

    private Bill selected() {
        int r = table.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Please select a bill first.");
            return null;
        }
        return rows.get(r);
    }

    private void addBill() {
        Bill b = showForm(this, null);
        if (b != null) UiStyle.guard(this, () -> { dao.insert(b); onChange.run(); });
    }

    private void editBill() {
        Bill old = selected();
        if (old == null) return;
        Bill updated = showForm(this, old);
        if (updated != null) UiStyle.guard(this, () -> { dao.update(updated); onChange.run(); });
    }

    private void markPaid() {
        Bill b = selected();
        if (b == null) return;
        if (b.paid()) {
            JOptionPane.showMessageDialog(this, "This bill is already marked as paid.");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
                "Mark \"" + b.name() + "\" as paid?\n"
                + "An expense of " + UiStyle.money(b.amount()) + " will be added automatically.",
                "Confirm payment", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            UiStyle.guard(this, () -> { service.markPaid(b); onChange.run(); });
        }
    }

    private void deleteBill() {
        Bill b = selected();
        if (b == null) return;
        int ok = JOptionPane.showConfirmDialog(this,
                "Delete bill \"" + b.name() + "\"?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (ok == JOptionPane.YES_OPTION) {
            UiStyle.guard(this, () -> { dao.delete(b.id()); onChange.run(); });
        }
    }

    private static Bill showForm(Component parent, Bill ex) {
        JTextField name = new JTextField(ex == null ? "" : ex.name(), 20);
        JTextField amount = new JTextField(ex == null ? "" : String.valueOf(ex.amount()));
        JTextField due = new JTextField(ex == null ? LocalDate.now().toString() : ex.dueDate().toString());
        JComboBox<Frequency> freq = new JComboBox<>(Frequency.values());
        if (ex != null) freq.setSelectedItem(ex.frequency());
        JTextField remind = new JTextField(ex == null ? "3" : String.valueOf(ex.reminderDays()));
        JTextField note = new JTextField(ex == null ? "" : ex.note());

        JPanel form = UiStyle.formPanel(
                new String[]{"Bill name", "Amount", "Due date (yyyy-MM-dd)",
                        "Frequency", "Remind me (days before)", "Note"},
                new JComponent[]{name, amount, due, freq, remind, note});

        while (true) {
            int ok = JOptionPane.showConfirmDialog(parent, form,
                    ex == null ? "Add Bill" : "Edit Bill",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (ok != JOptionPane.OK_OPTION) return null;
            try {
                String n = name.getText().trim();
                if (n.isEmpty()) throw new IllegalArgumentException("Bill name is required");

                double amt = Double.parseDouble(amount.getText().trim());
                if (amt <= 0) throw new IllegalArgumentException("Amount must be greater than 0");

                LocalDate d = LocalDate.parse(due.getText().trim());

                int days = Integer.parseInt(remind.getText().trim());
                if (days < 0) throw new IllegalArgumentException("Reminder days cannot be negative");

                return new Bill(ex == null ? 0 : ex.id(), n, amt, d,
                        (Frequency) freq.getSelectedItem(),
                        ex != null && ex.paid(), days, note.getText().trim());
            } catch (DateTimeException | IllegalArgumentException err) {
                JOptionPane.showMessageDialog(parent, "Invalid input: " + err.getMessage(),
                        "Check input", JOptionPane.WARNING_MESSAGE);
            }
        }
    }
}
