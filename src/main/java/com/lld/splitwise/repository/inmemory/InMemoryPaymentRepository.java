package com.lld.splitwise.repository.inmemory;

import com.lld.splitwise.model.Payment;
import com.lld.splitwise.repository.PaymentRepository;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryPaymentRepository implements PaymentRepository {
    private final List<Payment> payments = new CopyOnWriteArrayList<>();

    @Override
    public void save(Payment payment) { payments.add(payment); }

    @Override
    public List<Payment> findByGroup(String groupId) {
        return payments.stream().filter(p -> p.groupId().equals(groupId)).toList();
    }
}
