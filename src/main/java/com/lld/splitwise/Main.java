package com.lld.splitwise;

import com.lld.splitwise.controller.ApiResponse;
import com.lld.splitwise.dto.CreateExpenseRequest;
import com.lld.splitwise.dto.CreateGroupRequest;
import com.lld.splitwise.dto.ExpenseResponse;
import com.lld.splitwise.dto.SettleUpRequest;
import com.lld.splitwise.dto.SplitRequest;
import com.lld.splitwise.model.Transaction;
import com.lld.splitwise.model.User;

import java.math.BigDecimal;
import java.util.List;

/** End-to-end walkthrough of a Goa trip. Run: ./run.sh */
public class Main {

    public static void main(String[] args) {
        SplitwiseApplication app = new SplitwiseApplication(true);

        User rahul = app.userService.register("Rahul", "rahul@example.com");
        User priya = app.userService.register("Priya", "priya@example.com");
        User amit = app.userService.register("Amit", "amit@example.com");
        User sara = app.userService.register("Sara", "sara@example.com");

        String goa = app.groupController.createGroup(new CreateGroupRequest(rahul.getUserId(), "Goa Trip",
                List.of(priya.getUserId(), amit.getUserId(), sara.getUserId()))).body();
        section("Group created: Goa Trip (" + goa + ")");

        section("1. EQUAL: Rahul pays 1000.00 for dinner, split 3 ways (note the extra paisa)");
        print(app.expenseController.addExpense(new CreateExpenseRequest("k-dinner", rahul.getUserId(), goa,
                "Dinner", rahul.getUserId(), new BigDecimal("1000.00"), "EQUAL",
                List.of(SplitRequest.of(rahul.getUserId()), SplitRequest.of(priya.getUserId()),
                        SplitRequest.of(amit.getUserId())))));

        section("2. EXACT: Priya pays 1500.00 for the cab");
        print(app.expenseController.addExpense(new CreateExpenseRequest("k-cab", priya.getUserId(), goa,
                "Cab", priya.getUserId(), new BigDecimal("1500.00"), "EXACT",
                List.of(SplitRequest.of(priya.getUserId(), "300"), SplitRequest.of(amit.getUserId(), "700"),
                        SplitRequest.of(sara.getUserId(), "500")))));

        section("3. PERCENTAGE: Amit pays 2000.00 for the villa");
        print(app.expenseController.addExpense(new CreateExpenseRequest("k-villa", amit.getUserId(), goa,
                "Villa", amit.getUserId(), new BigDecimal("2000.00"), "PERCENTAGE",
                List.of(SplitRequest.of(rahul.getUserId(), "40"), SplitRequest.of(priya.getUserId(), "20"),
                        SplitRequest.of(amit.getUserId(), "20"), SplitRequest.of(sara.getUserId(), "20")))));

        section("4. SHARES: Sara pays 800.00 for drinks, Rahul had 2 shares");
        print(app.expenseController.addExpense(new CreateExpenseRequest("k-drinks", sara.getUserId(), goa,
                "Drinks", sara.getUserId(), new BigDecimal("800.00"), "SHARES",
                List.of(SplitRequest.of(rahul.getUserId(), "2"), SplitRequest.of(priya.getUserId(), "1"),
                        SplitRequest.of(sara.getUserId(), "1")))));

        section("5. IDEMPOTENCY: the client retries the dinner request after a timeout");
        CreateExpenseRequest retry = new CreateExpenseRequest("k-dinner", rahul.getUserId(), goa,
                "Dinner", rahul.getUserId(), new BigDecimal("1000.00"), "EQUAL",
                List.of(SplitRequest.of(rahul.getUserId()), SplitRequest.of(priya.getUserId()),
                        SplitRequest.of(amit.getUserId())));
        print(app.expenseController.addExpense(retry));
        System.out.println("  -> same expenseId as step 1 and no new notifications: nothing was double-counted");

        section("6. VALIDATION: percentages add up to 90");
        print(app.expenseController.addExpense(new CreateExpenseRequest("k-bad", rahul.getUserId(), goa,
                "Bad", rahul.getUserId(), new BigDecimal("100"), "PERCENTAGE",
                List.of(SplitRequest.of(rahul.getUserId(), "50"), SplitRequest.of(priya.getUserId(), "40")))));

        section("Net balances (+ gets back, - owes)");
        app.balanceController.groupBalances(rahul.getUserId(), goa).body()
                .forEach((u, b) -> System.out.printf("  %-6s %10s%n", u, b));

        section("7. SIMPLIFY DEBTS (suggestions only, ledger unchanged)");
        List<Transaction> plan = app.balanceController.suggestSettlements(rahul.getUserId(), goa).body();
        plan.forEach(t -> System.out.println("  " + t));

        section("8. SETTLE UP: record every suggested payment");
        int i = 0;
        for (Transaction t : plan) {
            print(app.balanceController.settleUp(new SettleUpRequest("pay-" + (i++), t.from().getUserId(), goa,
                    t.from().getUserId(), t.to().getUserId(), t.amount())));
        }

        section("Net balances after settle up");
        app.balanceController.groupBalances(rahul.getUserId(), goa).body()
                .forEach((u, b) -> System.out.printf("  %-6s %10s%n", u, b));

        section("9. Sara leaves the group (allowed: she is settled)");
        print(app.groupController.removeMember(rahul.getUserId(), goa, sara.getUserId()));

        section("Activity feed");
        app.activityFeed.feed(goa).forEach(e -> System.out.println("  " + e.type() + ": " + e.message()));
    }

    private static void section(String title) {
        System.out.println();
        System.out.println("== " + title);
    }

    private static void print(ApiResponse<?> r) {
        if (!r.isOk()) {
            System.out.println("  HTTP " + r.status() + ": " + r.error());
        } else if (r.body() instanceof ExpenseResponse e) {
            System.out.println("  HTTP 200 " + e.expenseId() + " '" + e.description() + "' " + e.amount()
                    + " shares=" + e.shareByUserId());
        } else {
            System.out.println("  HTTP 200 " + (r.body() == null ? "" : r.body()));
        }
    }
}
