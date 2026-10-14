package com.household.dao;

import com.household.db.DatabaseManager;
import com.household.model.Expense;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ExpenseDAO {

    private final DatabaseManager db = DatabaseManager.getInstance();

    public void insert(Expense e) {
        db.execute("INSERT INTO expenses (expense_date, title, category, amount, note) VALUES (?,?,?,?,?)",
                e.date().toString(), e.title(), e.category(), e.amount(), e.note());
    }

    public void update(Expense e) {
        db.execute("UPDATE expenses SET expense_date=?, title=?, category=?, amount=?, note=? WHERE id=?",
                e.date().toString(), e.title(), e.category(), e.amount(), e.note(), e.id());
    }

    public void delete(int id) {
        db.execute("DELETE FROM expenses WHERE id=?", id);
    }

    public List<Expense> findByMonth(YearMonth ym) {
        List<Expense> list = new ArrayList<>();
        String sql = "SELECT * FROM expenses WHERE expense_date BETWEEN ? AND ? "
                   + "ORDER BY expense_date DESC, id DESC";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bindRange(ps, ym);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return list;
    }

    public double totalByMonth(YearMonth ym) {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE expense_date BETWEEN ? AND ?";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bindRange(ps, ym);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Map<String, Double> categoryTotals(YearMonth ym) {
        Map<String, Double> map = new LinkedHashMap<>();
        String sql = "SELECT category, SUM(amount) AS total FROM expenses "
                   + "WHERE expense_date BETWEEN ? AND ? GROUP BY category ORDER BY total DESC";
        try (Connection c = db.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            bindRange(ps, ym);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(rs.getString("category"), rs.getDouble("total"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return map;
    }

    private void bindRange(PreparedStatement ps, YearMonth ym) throws SQLException {
        ps.setString(1, ym.atDay(1).toString());
        ps.setString(2, ym.atEndOfMonth().toString());
    }

    private Expense map(ResultSet rs) throws SQLException {
        return new Expense(
                rs.getInt("id"),
                LocalDate.parse(rs.getString("expense_date")),
                rs.getString("title"),
                rs.getString("category"),
                rs.getDouble("amount"),
                Objects.toString(rs.getString("note"), ""));
    }
}
