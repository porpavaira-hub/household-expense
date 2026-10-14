package com.household.service;

import com.household.dao.BillDAO;
import com.household.model.Bill;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

public class ReminderService {

    /** A bill that needs attention. daysLeft < 0 means overdue. */
    public record Alert(Bill bill, long daysLeft) {}

    private final BillDAO billDao = new BillDAO();

    public static long daysLeft(Bill b) {
        return ChronoUnit.DAYS.between(LocalDate.now(), b.dueDate());
    }

    /** Unpaid bills that are overdue or inside their reminder window. */
    public List<Alert> getAlerts() {
        return billDao.findAll().stream()
                .filter(b -> !b.paid())
                .map(b -> new Alert(b, daysLeft(b)))
                .filter(a -> a.daysLeft() <= a.bill().reminderDays())
                .sorted(Comparator.comparingLong(Alert::daysLeft))
                .toList();
    }
}
