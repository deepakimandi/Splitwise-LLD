package com.lld.splitwise.dto;

import java.math.BigDecimal;
import java.util.Map;

public record ExpenseResponse(String expenseId, String groupId, String description,
                              BigDecimal amount, Map<String, BigDecimal> shareByUserId) {
}
