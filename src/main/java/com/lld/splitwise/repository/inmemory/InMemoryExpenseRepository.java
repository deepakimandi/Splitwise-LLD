package com.lld.splitwise.repository.inmemory;

import com.lld.splitwise.model.Expense;
import com.lld.splitwise.repository.ExpenseRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryExpenseRepository implements ExpenseRepository {
    private final Map<String, Expense> expenses = new ConcurrentHashMap<>();

    @Override
    public void save(Expense expense) { expenses.put(expense.getExpenseId(), expense); }

    @Override
    public Optional<Expense> findById(String expenseId) { return Optional.ofNullable(expenses.get(expenseId)); }

    @Override
    public List<Expense> findByGroup(String groupId, int page, int size) {
        return expenses.values().stream()
                .filter(e -> e.getGroupId().equals(groupId))
                .sorted(Comparator.comparing(Expense::getCreatedAt).reversed())
                .skip((long) page * size)
                .limit(size)
                .toList();
    }

    @Override
    public Optional<Expense> delete(String expenseId) { return Optional.ofNullable(expenses.remove(expenseId)); }
}
