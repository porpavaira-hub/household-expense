package com.household.dao;

import com.household.db.DatabaseManager;
import com.household.model.Bill;
import com.household.model.Frequency;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BillDAO {

    private final DatabaseManager db = DatabaseManager.getInstance();

    public void insert(Bill b) {
        db.execute("INSERT INTO bills (name, amount, due_date, frequency, paid, reminder_days, note) "
                 + "VALUES (?,?,?,?,0,?,?)",
                b.name(), b.amount(), b.dueDate().toString(), b.frequency().name(),
                b.reminderDays(), b.note());
    }

    public void update(Bill b) {
        db.execute("UPDATE bills SET name=?, amount=?, due_date=?, frequency=?, reminder_days=?, note=? "
                 + "WHERE id=?",
                b.name(), b.amount(), b.dueDate().toString(), b.frequency().name(),
                b.reminderDays(), b.note(), b.id());
    }

    public void delete(int id) {
        db.execute("DELETE FROM bills WHERE id=?", id);
    }

    /** One-time bill -> marked paid. Recurring bill -> due date moves to next cycle. */
    public void markPaid(Bill b) {
        if (b.frequency() == Frequency.ONCE) {
            db.execute("UPDATE bills SET paid=1 WHERE id=?", b.id());
            return;
        }
        LocalDate today = LocalDate.now();
        LocalDate next = b.frequency().next(b.dueDate());
        while (!next.isAfter(today)) {
            next = b.frequency().next(next);
        }
        db.execute("UPDATE bills SET due_date=? WHERE id=?", next.toString(), b.id());
    }

    public List<Bill> findAll() {
        List<Bill> list = new ArrayList<>();
        String sql = "SELECT * FROM bills ORDER BY due_date ASC";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return list;
    }

    private Bill map(ResultSet rs) throws SQLException {
        return new Bill(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getDouble("amount"),
                LocalDate.parse(rs.getString("due_date")),
                Frequency.valueOf(rs.getString("frequency")),
                rs.getInt("paid") == 1,
                rs.getInt("reminder_days"),
                Objects.toString(rs.getString("note"), ""));
    }
}
