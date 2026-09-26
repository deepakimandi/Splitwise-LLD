package com.lld.splitwise.repository.inmemory;

import com.lld.splitwise.model.User;
import com.lld.splitwise.repository.UserRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserRepository implements UserRepository {
    private final Map<String, User> users = new ConcurrentHashMap<>();

    @Override
    public void save(User user) { users.put(user.getUserId(), user); }

    @Override
    public Optional<User> findById(String userId) { return Optional.ofNullable(users.get(userId)); }
}
