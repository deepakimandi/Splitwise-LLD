package com.lld.splitwise.repository;

import com.lld.splitwise.model.Expense;

import java.util.List;
import java.util.Optional;

public interface ExpenseRepository {
    void save(Expense expense);

    Optional<Expense> findById(String expenseId);

    /** Newest first, paginated: large groups are never loaded in one go. */
    List<Expense> findByGroup(String groupId, int page, int size);

    /** Atomic remove; returns empty if it was already deleted (guards double-delete). */
    Optional<Expense> delete(String expenseId);
}
