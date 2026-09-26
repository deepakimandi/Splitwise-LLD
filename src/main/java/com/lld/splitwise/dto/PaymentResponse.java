package com.lld.splitwise.dto;

import java.math.BigDecimal;

public record PaymentResponse(String paymentId, String groupId, String fromUserId, String toUserId, BigDecimal amount) {
}
