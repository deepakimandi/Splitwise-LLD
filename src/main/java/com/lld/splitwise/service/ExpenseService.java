package com.lld.splitwise.service;

import com.lld.splitwise.dto.CreateExpenseRequest;
import com.lld.splitwise.dto.ExpenseResponse;
import com.lld.splitwise.exception.NotFoundException;
import com.lld.splitwise.idempotency.IdempotencyStore;
import com.lld.splitwise.model.Expense;
import com.lld.splitwise.model.Group;
import com.lld.splitwise.model.Split;
import com.lld.splitwise.model.SplitInput;
import com.lld.splitwise.model.SplitType;
import com.lld.splitwise.model.User;
import com.lld.splitwise.observer.EventPublisher;
import com.lld.splitwise.observer.EventType;
import com.lld.splitwise.observer.SplitwiseEvent;
import com.lld.splitwise.repository.ExpenseRepository;
import com.lld.splitwise.repository.GroupRepository;
import com.lld.splitwise.repository.UserRepository;
import com.lld.splitwise.strategy.SplitStrategyFactory;
import com.lld.splitwise.validation.RequestValidator;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Orchestrates the "add expense" use case. It holds no business rules of its own:
 * splitting lives in strategies, invariants in Group/BalanceSheet, retries in IdempotencyStore.
 * All collaborators come in through the constructor (Dependency Injection, no Singletons).
 */
public class ExpenseService {
    private final ExpenseRepository expenses;
    private final Lookup lookup;
    private final SplitStrategyFactory strategies;
    private final IdempotencyStore idempotency;
    private final EventPublisher events;
    private final IdGenerator ids;

    public ExpenseService(ExpenseRepository expenses, UserRepository users, GroupRepository groups,
                          SplitStrategyFactory strategies, IdempotencyStore idempotency,
                          EventPublisher events, IdGenerator ids) {
        this.expenses = expenses;
        this.lookup = new Lookup(users, groups);
        this.strategies = strategies;
        this.idempotency = idempotency;
        this.events = events;
        this.ids = ids;
    }

    public ExpenseResponse addExpense(CreateExpenseRequest request) {
        SplitType type = RequestValidator.validate(request);                       // 1. shape check
        String scopedKey = request.requesterId() + ":" + request.idempotencyKey();
        String requestHash = Integer.toHexString(request.hashCode());              // prod: SHA-256 of body
        return idempotency.execute(scopedKey, requestHash, () -> doAddExpense(request, type));
    }

    private ExpenseResponse doAddExpense(CreateExpenseRequest request, SplitType type) {
        Group group = lookup.group(request.groupId());                              // 2. load aggregates
        lookup.requireMember(group, request.requesterId());                        //    + authorization
        User requester = lookup.user(request.requesterId());
        User paidBy = lookup.user(request.paidByUserId());
        List<SplitInput> inputs = request.splits().stream()
                .map(s -> new SplitInput(lookup.user(s.userId()), s.value()))
                .toList();

        BigDecimal amount = request.amount();
        List<Split> splits = strategies.get(type).split(amount, inputs);            // 3. Strategy

        Expense expense = Expense.builder()                                         // 4. Builder
                .expenseId(ids.next("E"))
                .groupId(group.getGroupId())
                .description(request.description())
                .paidBy(paidBy)
                .amount(amount)
                .splitType(type)
                .splits(splits)
                .createdBy(requester)
                .build();

        group.recordExpense(expense);          // 5. ledger update: locks + membership check (atomic)
        expenses.save(expense);                // (with a DB, steps 5 and 6 share one transaction)

        events.publish(SplitwiseEvent.of(EventType.EXPENSE_ADDED, group.getGroupId(), requester,  // 6. Observer
                participants(expense), requester + " added '" + expense.getDescription() + "' ("
                        + amount + ", paid by " + paidBy + ") in " + group.getName()));
        return toResponse(expense);
    }

    /** Delete = post the reverse entries. Edit = delete + add a new expense (keeps an audit trail). */
    public void deleteExpense(String requesterId, String expenseId) {
        Expense expense = expenses.findById(expenseId).orElseThrow(() -> new NotFoundException("Expense", expenseId));
        Group group = lookup.group(expense.getGroupId());
        lookup.requireMember(group, requesterId);
        // delete() is atomic, so two concurrent deletes cannot both revert the ledger
        if (expenses.delete(expenseId).isEmpty()) {
            throw new NotFoundException("Expense", expenseId);
        }
        group.revertExpense(expense);
        User actor = lookup.user(requesterId);
        events.publish(SplitwiseEvent.of(EventType.EXPENSE_DELETED, group.getGroupId(), actor,
                participants(expense), actor + " deleted '" + expense.getDescription() + "'"));
    }

    public List<ExpenseResponse> listExpenses(String requesterId, String groupId, int page, int size) {
        Group group = lookup.group(groupId);
        lookup.requireMember(group, requesterId);
        return expenses.findByGroup(groupId, page, size).stream().map(this::toResponse).toList();
    }

    private static Set<User> participants(Expense e) {
        Set<User> users = e.getSplits().stream().map(Split::user).collect(Collectors.toSet());
        users.add(e.getPaidBy());
        return users;
    }

    private ExpenseResponse toResponse(Expense e) {
        Map<String, BigDecimal> shares = new LinkedHashMap<>();
        e.getSplits().forEach(s -> shares.put(s.user().getUserId(), s.amount()));
        return new ExpenseResponse(e.getExpenseId(), e.getGroupId(), e.getDescription(), e.getAmount(), shares);
    }
}
