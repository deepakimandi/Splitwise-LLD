package com.lld.splitwise.service;

import com.lld.splitwise.exception.NotFoundException;
import com.lld.splitwise.model.User;
import com.lld.splitwise.repository.UserRepository;

public class UserService {
    private final UserRepository users;
    private final IdGenerator ids;

    public UserService(UserRepository users, IdGenerator ids) {
        this.users = users;
        this.ids = ids;
    }

    public User register(String name, String email) {
        User user = new User(ids.next("U"), name, email);
        users.save(user);
        return user;
    }

    public User get(String userId) {
        return users.findById(userId).orElseThrow(() -> new NotFoundException("User", userId));
    }
}
