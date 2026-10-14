package com.household.model;

import java.time.LocalDate;

public enum Frequency {
    ONCE("One-time"),
    MONTHLY("Monthly"),
    YEARLY("Yearly");

    private final String label;

    Frequency(String label) {
        this.label = label;
    }

    /** Returns the next due date for this frequency. */
    public LocalDate next(LocalDate date) {
        return switch (this) {
            case MONTHLY -> date.plusMonths(1);
            case YEARLY  -> date.plusYears(1);
            case ONCE    -> date;
        };
    }

    @Override
    public String toString() {
        return label;
    }
}
