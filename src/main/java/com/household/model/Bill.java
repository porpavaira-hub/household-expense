package com.household.model;

import java.time.LocalDate;

public record Bill(int id,
                   String name,
                   double amount,
                   LocalDate dueDate,
                   Frequency frequency,
                   boolean paid,
                   int reminderDays,
                   String note) {}
