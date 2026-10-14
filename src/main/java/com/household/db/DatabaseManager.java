package com.household.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public final class DatabaseManager {

    private static final String URL = "jdbc:sqlite:household.db";
    private static DatabaseManager instance;

    private DatabaseManager() {
        createTables();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    /** Runs INSERT / UPDATE / DELETE with the given parameters. */
    public void execute(String sql, Object... params) {
        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }

    private void createTables() {
        String expenses = """
            CREATE TABLE IF NOT EXISTS expenses (
                id           INTEGER PRIMARY KEY AUTOINCREMENT,
                expense_date TEXT    NOT NULL,
                title        TEXT    NOT NULL,
                category     TEXT    NOT NULL,
                amount       REAL    NOT NULL CHECK (amount > 0),
                note         TEXT
            )
            """;

        String bills = """
            CREATE TABLE IF NOT EXISTS bills (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                name          TEXT    NOT NULL,
                amount        REAL    NOT NULL CHECK (amount > 0),
                due_date      TEXT    NOT NULL,
                frequency     TEXT    NOT NULL,
                paid          INTEGER NOT NULL DEFAULT 0,
                reminder_days INTEGER NOT NULL DEFAULT 3,
                note          TEXT
            )
            """;

        try (Connection c = getConnection(); Statement st = c.createStatement()) {
            st.execute(expenses);
            st.execute(bills);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create tables", e);
        }
    }
}
