package com.lld.splitwise.repository;

import com.lld.splitwise.model.Payment;

import java.util.List;

public interface PaymentRepository {
    void save(Payment payment);

    List<Payment> findByGroup(String groupId);
}
