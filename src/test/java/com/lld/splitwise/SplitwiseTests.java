package com.lld.splitwise;

import com.lld.splitwise.controller.ApiResponse;
import com.lld.splitwise.dto.CreateExpenseRequest;
import com.lld.splitwise.dto.CreateGroupRequest;
import com.lld.splitwise.dto.ExpenseResponse;
import com.lld.splitwise.dto.SettleUpRequest;
import com.lld.splitwise.dto.SplitRequest;
import com.lld.splitwise.model.Money;
import com.lld.splitwise.model.Split;
import com.lld.splitwise.model.SplitInput;
import com.lld.splitwise.model.Transaction;
import com.lld.splitwise.model.User;
import com.lld.splitwise.settlement.DebtSimplifier;
import com.lld.splitwise.strategy.PercentageSplitStrategy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dependency-free tests (plain main + assertions) so the project runs with just a JDK.
 * Run: ./run.sh test
 */
public class SplitwiseTests {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        run("equal split never loses a paisa", SplitwiseTests::equalSplitRemainder);
        run("percentage split rounds and still sums to total", SplitwiseTests::percentageRounding);
        run("exact split that doesn't add up is rejected (400)", SplitwiseTests::exactMismatch);
        run("non-member requester gets 403, non-member participant gets 400", SplitwiseTests::membership);
        run("idempotent retry returns the same expense", SplitwiseTests::idempotentReplay);
        run("same key with different payload is rejected (409)", SplitwiseTests::idempotencyKeyMisuse);
        run("concurrent retries with one key create exactly one expense", SplitwiseTests::idempotencyRace);
        run("parallel expenses: no lost updates, no deadlock", SplitwiseTests::concurrentExpenses);
        run("readers never see a half-applied expense", SplitwiseTests::snapshotConsistency);
        run("debt simplification settles everyone in <= N-1 payments", SplitwiseTests::simplification);
        run("member with a balance cannot leave (409)", SplitwiseTests::removeMemberWithBalance);
        run("delete reverts the ledger; double delete is 404", SplitwiseTests::deleteExpense);
        run("user summary aggregates across groups", SplitwiseTests::userSummary);

        System.out.println();
        System.out.println(passed + " passed, " + failed + " failed");
        if (failed > 0) System.exit(1);
    }

    // ---------------------------------------------------------------- tests

    static void equalSplitRemainder() {
        Fixture f = new Fixture(3);
        ExpenseResponse r = f.addEqual("k1", f.u(0), "100.00", 0, 1, 2).body();
        check(r.shareByUserId().values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .compareTo(new BigDecimal("100.00")) == 0, "shares must sum to 100.00");
        check(r.shareByUserId().get(f.id(0)).equals(new BigDecimal("33.34")), "first participant gets extra paisa");
    }

    static void percentageRounding() {
        User a = new User("a", "A", null), b = new User("b", "B", null), c = new User("c", "C", null);
        List<Split> splits = new PercentageSplitStrategy().split(new BigDecimal("10.00"), List.of(
                new SplitInput(a, new BigDecimal("33.33")), new SplitInput(b, new BigDecimal("33.33")),
                new SplitInput(c, new BigDecimal("33.34"))));
        BigDecimal sum = splits.stream().map(Split::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        check(sum.compareTo(new BigDecimal("10.00")) == 0, "sum was " + sum);
    }

    static void exactMismatch() {
        Fixture f = new Fixture(2);
        ApiResponse<ExpenseResponse> r = f.app.expenseController.addExpense(new CreateExpenseRequest("k", f.id(0),
                f.group, "x", f.id(0), new BigDecimal("100"), "EXACT",
                List.of(SplitRequest.of(f.id(0), "60"), SplitRequest.of(f.id(1), "30"))));
        check(r.status() == 400, "expected 400, got " + r.status());
    }

    static void membership() {
        Fixture f = new Fixture(2);
        User outsider = f.app.userService.register("Outsider", null);
        ApiResponse<ExpenseResponse> asOutsider = f.app.expenseController.addExpense(new CreateExpenseRequest("k",
                outsider.getUserId(), f.group, "x", f.id(0), new BigDecimal("10"), "EQUAL",
                List.of(SplitRequest.of(f.id(0)), SplitRequest.of(f.id(1)))));
        check(asOutsider.status() == 403, "requester not in group: expected 403, got " + asOutsider.status());

        ApiResponse<ExpenseResponse> withOutsider = f.app.expenseController.addExpense(new CreateExpenseRequest("k2",
                f.id(0), f.group, "x", f.id(0), new BigDecimal("10"), "EQUAL",
                List.of(SplitRequest.of(f.id(0)), SplitRequest.of(outsider.getUserId()))));
        check(withOutsider.status() == 400, "participant not in group: expected 400, got " + withOutsider.status());
        check(f.net(0).signum() == 0, "rejected expense must not touch the ledger");
    }

    static void idempotentReplay() {
        Fixture f = new Fixture(2);
        String first = f.addEqual("same-key", f.u(0), "50", 0, 1).body().expenseId();
        String second = f.addEqual("same-key", f.u(0), "50", 0, 1).body().expenseId();
        check(first.equals(second), "retry must return the original expense");
        check(f.net(0).compareTo(new BigDecimal("25.00")) == 0, "balance counted once, was " + f.net(0));
    }

    static void idempotencyKeyMisuse() {
        Fixture f = new Fixture(2);
        f.addEqual("key", f.u(0), "50", 0, 1);
        ApiResponse<ExpenseResponse> r = f.addEqual("key", f.u(0), "70", 0, 1);
        check(r.status() == 409, "expected 409, got " + r.status());
    }

    static void idempotencyRace() throws Exception {
        Fixture f = new Fixture(2);
        int threads = 16;
        CountDownLatch start = new CountDownLatch(1);
        Set<String> expenseIds = ConcurrentHashMap.newKeySet();
        AtomicInteger conflicts = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            pool.submit(() -> {
                start.await();
                ApiResponse<ExpenseResponse> r = f.addEqual("race", f.u(0), "100", 0, 1);
                if (r.isOk()) expenseIds.add(r.body().expenseId());
                else if (r.status() == 409) conflicts.incrementAndGet();
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        check(pool.awaitTermination(10, TimeUnit.SECONDS), "timed out");
        check(expenseIds.size() == 1, "expected exactly one expense id, got " + expenseIds);
        check(f.net(0).compareTo(new BigDecimal("50.00")) == 0, "ledger applied more than once: " + f.net(0));
    }

    static void concurrentExpenses() throws Exception {
        int users = 6, threads = 8, perThread = 400;
        Fixture f = new Fixture(users);
        Map<String, BigDecimal> expected = new ConcurrentHashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        for (int t = 0; t < threads; t++) {
            final int seed = t;
            pool.submit(() -> {
                Random rnd = new Random(seed);
                start.await();
                for (int i = 0; i < perThread; i++) {
                    int payer = rnd.nextInt(users);
                    int other = (payer + 1 + rnd.nextInt(users - 1)) % users;
                    // payer pays 10.00 split equally with "other": other owes payer 5.00
                    ApiResponse<ExpenseResponse> r = f.addEqual("c-" + seed + "-" + i, f.u(payer), "10.00", payer, other);
                    if (!r.isOk()) throw new IllegalStateException(r.error());
                    expected.merge(f.id(payer), new BigDecimal("5.00"), BigDecimal::add);
                    expected.merge(f.id(other), new BigDecimal("-5.00"), BigDecimal::add);
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        check(pool.awaitTermination(30, TimeUnit.SECONDS), "possible deadlock: workers did not finish");

        Map<User, BigDecimal> actual = f.app.balanceService.groupBalances(f.id(0), f.group);
        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<User, BigDecimal> e : actual.entrySet()) {
            BigDecimal exp = expected.getOrDefault(e.getKey().getUserId(), BigDecimal.ZERO);
            check(e.getValue().compareTo(exp) == 0, e.getKey() + ": expected " + exp + " got " + e.getValue());
            total = total.add(e.getValue());
        }
        check(total.signum() == 0, "nets must sum to zero, got " + total);
    }

    /**
     * Each cell update is already atomic (ConcurrentHashMap.merge), so lost updates are impossible even
     * without locks. What the per-user locks add is ATOMICITY OF THE WHOLE EXPENSE: a reader holding the
     * same locks can never observe "creditor updated, debtor not yet", which would break sum(nets) == 0.
     */
    static void snapshotConsistency() throws Exception {
        int users = 4;
        Fixture f = new Fixture(users);
        AtomicInteger violations = new AtomicInteger();
        AtomicBoolean writing = new AtomicBoolean(true);
        Thread reader = new Thread(() -> {
            while (writing.get()) {
                BigDecimal sum = f.app.balanceService.groupBalances(f.id(0), f.group).values().stream()
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                if (sum.signum() != 0) violations.incrementAndGet();
            }
        });
        reader.start();

        ExecutorService writers = Executors.newFixedThreadPool(4);
        for (int t = 0; t < 4; t++) {
            final int seed = t;
            writers.submit(() -> {
                Random rnd = new Random(seed);
                for (int i = 0; i < 3000; i++) {
                    f.addEqual("s-" + seed + "-" + i, f.u(rnd.nextInt(users)), "9.99", 0, 1, 2, 3);
                }
            });
        }
        writers.shutdown();
        check(writers.awaitTermination(30, TimeUnit.SECONDS), "writers timed out");
        writing.set(false);
        reader.join();
        check(violations.get() == 0, violations.get() + " inconsistent snapshots observed");
    }

    static void simplification() {
        Random rnd = new Random(42);
        for (int round = 0; round < 200; round++) {
            int n = 2 + rnd.nextInt(8);
            Map<User, BigDecimal> net = new HashMap<>();
            BigDecimal sum = BigDecimal.ZERO;
            List<User> people = new ArrayList<>();
            for (int i = 0; i < n; i++) people.add(new User("p" + i, "P" + i, null));
            for (int i = 0; i < n - 1; i++) {
                BigDecimal v = BigDecimal.valueOf(rnd.nextInt(20001) - 10000, 2);
                net.put(people.get(i), v);
                sum = sum.add(v);
            }
            net.put(people.get(n - 1), sum.negate());    // nets always sum to zero

            List<Transaction> plan = new DebtSimplifier().simplify(net);
            check(plan.size() <= n - 1, "too many transfers: " + plan.size());
            Map<User, BigDecimal> after = new HashMap<>(net);
            for (Transaction t : plan) {
                check(Money.isPositive(t.amount()), "non-positive transfer");
                after.merge(t.from(), t.amount(), BigDecimal::add);
                after.merge(t.to(), t.amount().negate(), BigDecimal::add);
            }
            after.values().forEach(v -> check(v.signum() == 0, "not settled: " + after));
        }
    }

    static void removeMemberWithBalance() {
        Fixture f = new Fixture(2);
        f.addEqual("k", f.u(0), "100", 0, 1);
        check(f.app.groupController.removeMember(f.id(0), f.group, f.id(1)).status() == 409, "expected 409");
        f.app.balanceController.settleUp(new SettleUpRequest("p", f.id(1), f.group, f.id(1), f.id(0),
                new BigDecimal("50")));
        check(f.app.groupController.removeMember(f.id(0), f.group, f.id(1)).isOk(), "settled member should leave");
    }

    static void deleteExpense() {
        Fixture f = new Fixture(3);
        String id = f.addEqual("k", f.u(0), "90", 0, 1, 2).body().expenseId();
        check(f.net(0).compareTo(new BigDecimal("60.00")) == 0, "before delete");
        check(f.app.expenseController.deleteExpense(f.id(1), id).isOk(), "delete should succeed");
        check(f.net(0).signum() == 0 && f.net(1).signum() == 0, "ledger must be back to zero");
        check(f.app.expenseController.deleteExpense(f.id(1), id).status() == 404, "second delete must be 404");
    }

    static void userSummary() {
        Fixture f = new Fixture(2);
        String second = f.app.groupController.createGroup(
                new CreateGroupRequest(f.id(0), "Flat", List.of(f.id(1)))).body();
        f.addEqual("a", f.u(0), "100", 0, 1);                      // user0 +50 in group 1
        f.app.expenseController.addExpense(new CreateExpenseRequest("b", f.id(1), second, "Rent", f.id(1),
                new BigDecimal("30"), "EQUAL", List.of(SplitRequest.of(f.id(0)), SplitRequest.of(f.id(1)))));
        BigDecimal overall = f.app.balanceController.userSummary(f.id(0)).body().overallNet();
        check(overall.compareTo(new BigDecimal("35.00")) == 0, "expected 35.00, got " + overall);   // 50 - 15
    }

    // ---------------------------------------------------------------- helpers

    /** Fresh app + n users all in one group. */
    static final class Fixture {
        final SplitwiseApplication app = new SplitwiseApplication(false);
        final List<User> users = new ArrayList<>();
        final String group;

        Fixture(int n) {
            for (int i = 0; i < n; i++) users.add(app.userService.register("User" + i, null));
            List<String> others = users.subList(1, n).stream().map(User::getUserId).toList();
            group = app.groupController.createGroup(new CreateGroupRequest(id(0), "Test", others)).body();
        }

        User u(int i) { return users.get(i); }

        String id(int i) { return users.get(i).getUserId(); }

        BigDecimal net(int i) {
            return app.balanceService.groupBalances(id(0), group).getOrDefault(u(i), BigDecimal.ZERO);
        }

        ApiResponse<ExpenseResponse> addEqual(String key, User payer, String amount, int... participants) {
            List<SplitRequest> splits = new ArrayList<>();
            for (int p : participants) splits.add(SplitRequest.of(id(p)));
            return app.expenseController.addExpense(new CreateExpenseRequest(key, payer.getUserId(), group,
                    "e", payer.getUserId(), new BigDecimal(amount), "EQUAL", splits));
        }
    }

    interface TestBody {
        void run() throws Exception;
    }

    static void run(String name, TestBody body) {
        try {
            body.run();
            passed++;
            System.out.println("PASS  " + name);
        } catch (Throwable t) {
            failed++;
            System.out.println("FAIL  " + name + " -> " + t.getMessage());
        }
    }

    static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
