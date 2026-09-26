package com.lld.splitwise.controller;

import com.lld.splitwise.dto.PaymentResponse;
import com.lld.splitwise.dto.SettleUpRequest;
import com.lld.splitwise.dto.UserBalanceSummary;
import com.lld.splitwise.model.Transaction;
import com.lld.splitwise.model.User;
import com.lld.splitwise.service.BalanceService;
import com.lld.splitwise.service.SettlementService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class BalanceController extends BaseController {
    private final BalanceService balanceService;
    private final SettlementService settlementService;

    public BalanceController(BalanceService balanceService, SettlementService settlementService) {
        this.balanceService = balanceService;
        this.settlementService = settlementService;
    }

    /** GET /groups/{groupId}/balances */
    public ApiResponse<Map<User, BigDecimal>> groupBalances(String requesterId, String groupId) {
        return handle(() -> balanceService.groupBalances(requesterId, groupId));
    }

    /** GET /users/{userId}/summary */
    public ApiResponse<UserBalanceSummary> userSummary(String userId) {
        return handle(() -> balanceService.userSummary(userId));
    }

    /** GET /groups/{groupId}/settlements/suggested */
    public ApiResponse<List<Transaction>> suggestSettlements(String requesterId, String groupId) {
        return handle(() -> settlementService.suggestSettlements(requesterId, groupId));
    }

    /** POST /groups/{groupId}/payments   (header: Idempotency-Key) */
    public ApiResponse<PaymentResponse> settleUp(SettleUpRequest request) {
        return handle(() -> settlementService.settleUp(request));
    }
}
