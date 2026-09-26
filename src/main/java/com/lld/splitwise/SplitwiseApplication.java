package com.lld.splitwise;

import com.lld.splitwise.controller.BalanceController;
import com.lld.splitwise.controller.ExpenseController;
import com.lld.splitwise.controller.GroupController;
import com.lld.splitwise.idempotency.IdempotencyStore;
import com.lld.splitwise.observer.ActivityFeedListener;
import com.lld.splitwise.observer.ConsoleNotificationChannel;
import com.lld.splitwise.observer.EventPublisher;
import com.lld.splitwise.observer.NotificationListener;
import com.lld.splitwise.repository.ExpenseRepository;
import com.lld.splitwise.repository.GroupRepository;
import com.lld.splitwise.repository.PaymentRepository;
import com.lld.splitwise.repository.UserRepository;
import com.lld.splitwise.repository.inmemory.InMemoryExpenseRepository;
import com.lld.splitwise.repository.inmemory.InMemoryGroupRepository;
import com.lld.splitwise.repository.inmemory.InMemoryPaymentRepository;
import com.lld.splitwise.repository.inmemory.InMemoryUserRepository;
import com.lld.splitwise.service.BalanceService;
import com.lld.splitwise.service.ExpenseService;
import com.lld.splitwise.service.GroupService;
import com.lld.splitwise.service.IdGenerator;
import com.lld.splitwise.service.SettlementService;
import com.lld.splitwise.service.UserService;
import com.lld.splitwise.settlement.DebtSimplifier;
import com.lld.splitwise.strategy.SplitStrategyFactory;

/**
 * Composition root: the ONE place where concrete classes are chosen and wired together.
 * This replaces the Singleton BalanceSheetManager from the first draft:
 *  - no hidden global state, so every test gets a fresh, isolated application,
 *  - swapping InMemory* for JPA repositories changes only this class.
 * (In Spring Boot, the container does this wiring for you.)
 */
public class SplitwiseApplication {

    public final UserService userService;
    public final GroupService groupService;
    public final ExpenseService expenseService;
    public final SettlementService settlementService;
    public final BalanceService balanceService;

    public final GroupController groupController;
    public final ExpenseController expenseController;
    public final BalanceController balanceController;

    public final ConsoleNotificationChannel notifications;
    public final ActivityFeedListener activityFeed;

    public SplitwiseApplication(boolean printNotifications) {
        UserRepository users = new InMemoryUserRepository();
        GroupRepository groups = new InMemoryGroupRepository();
        ExpenseRepository expenses = new InMemoryExpenseRepository();
        PaymentRepository payments = new InMemoryPaymentRepository();

        IdGenerator ids = new IdGenerator();
        IdempotencyStore idempotency = new IdempotencyStore();
        SplitStrategyFactory strategies = SplitStrategyFactory.withDefaults();

        EventPublisher events = EventPublisher.synchronous();
        notifications = new ConsoleNotificationChannel(printNotifications);
        activityFeed = new ActivityFeedListener();
        events.subscribe(new NotificationListener(notifications));
        events.subscribe(activityFeed);

        userService = new UserService(users, ids);
        groupService = new GroupService(groups, users, ids, events);
        expenseService = new ExpenseService(expenses, users, groups, strategies, idempotency, events, ids);
        settlementService = new SettlementService(payments, users, groups, new DebtSimplifier(), idempotency, events, ids);
        balanceService = new BalanceService(groups, users);

        groupController = new GroupController(groupService);
        expenseController = new ExpenseController(expenseService);
        balanceController = new BalanceController(balanceService, settlementService);
    }
}
