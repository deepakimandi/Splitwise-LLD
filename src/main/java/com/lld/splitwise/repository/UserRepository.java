package com.lld.splitwise.repository;

import com.lld.splitwise.model.User;

import java.util.Optional;

/** Persistence abstraction (Dependency Inversion): services depend on this, not on a DB. */
public interface UserRepository {
    void save(User user);

    Optional<User> findById(String userId);
}
