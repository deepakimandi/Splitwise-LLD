package com.lld.splitwise.controller;

import com.lld.splitwise.dto.CreateExpenseRequest;
import com.lld.splitwise.dto.ExpenseResponse;
import com.lld.splitwise.service.ExpenseService;

import java.util.List;

/** Thin: parse request, call service, map result. No business logic here. */
public class ExpenseController extends BaseController {
    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    /** POST /groups/{groupId}/expenses   (header: Idempotency-Key) */
    public ApiResponse<ExpenseResponse> addExpense(CreateExpenseRequest request) {
        return handle(() -> expenseService.addExpense(request));
    }

    /** DELETE /expenses/{expenseId} */
    public ApiResponse<Void> deleteExpense(String requesterId, String expenseId) {
        return handle(() -> {
            expenseService.deleteExpense(requesterId, expenseId);
            return null;
        });
    }

    /** GET /groups/{groupId}/expenses?page=0&size=20 */
    public ApiResponse<List<ExpenseResponse>> listExpenses(String requesterId, String groupId, int page, int size) {
        return handle(() -> expenseService.listExpenses(requesterId, groupId, page, size));
    }
}
