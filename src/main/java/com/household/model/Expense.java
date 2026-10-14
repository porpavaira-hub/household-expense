package com.household.model;

import java.time.LocalDate;

public record Expense(int id, LocalDate date, String title, String category, double amount, String note) {}
