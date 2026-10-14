package com.household.service;

import com.household.dao.BillDAO;
import com.household.dao.ExpenseDAO;
import com.household.model.Bill;
import com.household.model.Expense;

import java.time.LocalDate;

public class BillService {

    private final BillDAO billDao = new BillDAO();
    private final ExpenseDAO expenseDao = new ExpenseDAO();

    /** Marks the bill as paid and records it as an expense automatically. */
    public void markPaid(Bill bill) {
        billDao.markPaid(bill);
        expenseDao.insert(new Expense(0, LocalDate.now(), bill.name(), "Bills",
                bill.amount(), "Auto-added from bill payment"));
    }
}
