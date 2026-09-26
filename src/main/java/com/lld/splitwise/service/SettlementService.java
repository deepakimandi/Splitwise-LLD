package com.lld.splitwise.service;

import com.lld.splitwise.dto.PaymentResponse;
import com.lld.splitwise.dto.SettleUpRequest;
import com.lld.splitwise.idempotency.IdempotencyStore;
import com.lld.splitwise.model.Group;
import com.lld.splitwise.model.Money;
import com.lld.splitwise.model.Payment;
import com.lld.splitwise.model.Transaction;
import com.lld.splitwise.model.User;
import com.lld.splitwise.observer.EventPublisher;
import com.lld.splitwise.observer.EventType;
import com.lld.splitwise.observer.SplitwiseEvent;
import com.lld.splitwise.repository.GroupRepository;
import com.lld.splitwise.repository.PaymentRepository;
import com.lld.splitwise.repository.UserRepository;
import com.lld.splitwise.settlement.DebtSimplifier;
import com.lld.splitwise.validation.RequestValidator;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Two separate things, often confused in interviews:
 *  - suggestSettlements(): READ-ONLY. Runs debt simplification and returns who should pay whom.
 *  - settleUp():           WRITE. Records a real payment, which changes the ledger.
 */
public class SettlementService {
    private final PaymentRepository payments;
    private final Lookup lookup;
    private final DebtSimplifier simplifier;
    private final IdempotencyStore idempotency;
    private final EventPublisher events;
    private final IdGenerator ids;

    public SettlementService(PaymentRepository payments, UserRepository users, GroupRepository groups,
                             DebtSimplifier simplifier, IdempotencyStore idempotency,
                             EventPublisher events, IdGenerator ids) {
        this.payments = payments;
        this.lookup = new Lookup(users, groups);
        this.simplifier = simplifier;
        this.idempotency = idempotency;
        this.events = events;
        this.ids = ids;
    }

    public List<Transaction> suggestSettlements(String requesterId, String groupId) {
        Group group = lookup.group(groupId);
        lookup.requireMember(group, requesterId);
        return simplifier.simplify(group.netBalances());     // netBalances() is a consistent snapshot
    }

    public PaymentResponse settleUp(SettleUpRequest request) {
        RequestValidator.validate(request);
        String scopedKey = request.requesterId() + ":" + request.idempotencyKey();
        return idempotency.execute(scopedKey, Integer.toHexString(request.hashCode()), () -> doSettleUp(request));
    }

    private PaymentResponse doSettleUp(SettleUpRequest request) {
        Group group = lookup.group(request.groupId());
        lookup.requireMember(group, request.requesterId());
        User actor = lookup.user(request.requesterId());
        User from = lookup.user(request.fromUserId());
        User to = lookup.user(request.toUserId());

        Payment payment = new Payment(ids.next("P"), group.getGroupId(), from, to,
                Money.normalize(request.amount()), Instant.now());
        group.recordPayment(payment);
        payments.save(payment);

        events.publish(SplitwiseEvent.of(EventType.PAYMENT_RECORDED, group.getGroupId(), actor,
                Set.of(from, to), from + " paid " + to + " " + payment.amount() + " in " + group.getName()));
        return new PaymentResponse(payment.paymentId(), group.getGroupId(), from.getUserId(), to.getUserId(),
                payment.amount());
    }
}
