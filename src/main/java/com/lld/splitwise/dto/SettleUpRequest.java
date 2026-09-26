package com.lld.splitwise.dto;

import java.math.BigDecimal;

/** "fromUserId paid toUserId amount" - recorded by either party. Idempotent like expenses. */
public record SettleUpRequest(String idempotencyKey, String requesterId, String groupId,
                              String fromUserId, String toUserId, BigDecimal amount) {
}
