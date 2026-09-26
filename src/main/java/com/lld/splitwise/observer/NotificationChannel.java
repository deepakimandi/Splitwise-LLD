package com.lld.splitwise.observer;

import com.lld.splitwise.model.User;

/** How a notification is delivered (email, push, SMS...). Another small Strategy. */
public interface NotificationChannel {
    void send(User recipient, String message);
}
