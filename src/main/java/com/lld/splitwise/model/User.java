package com.lld.splitwise.model;

import java.util.Objects;

/**
 * A registered user. Identity is the userId only, so equals/hashCode use nothing else
 * (safe to use as a map key even if name/email change).
 */
public class User {
    private final String userId;
    private final String name;
    private final String email;

    public User(String userId, String name, String email) {
        this.userId = Objects.requireNonNull(userId);
        this.name = Objects.requireNonNull(name);
        this.email = email;
    }

    public String getUserId() { return userId; }
    public String getName()   { return name; }
    public String getEmail()  { return email; }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof User u && userId.equals(u.userId));
    }

    @Override
    public int hashCode() { return userId.hashCode(); }

    @Override
    public String toString() { return name; }
}
