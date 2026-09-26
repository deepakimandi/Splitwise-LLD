package com.lld.splitwise.observer;

import com.lld.splitwise.model.User;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Demo channel: prints and remembers messages (so tests can assert on them). */
public class ConsoleNotificationChannel implements NotificationChannel {

    private final List<String> sent = new CopyOnWriteArrayList<>();
    private final boolean print;

    public ConsoleNotificationChannel(boolean print) {
        this.print = print;
    }

    @Override
    public void send(User recipient, String message) {
        String line = "[notify " + recipient + "] " + message;
        sent.add(line);
        if (print) {
            System.out.println("    " + line);
        }
    }

    public List<String> sentMessages() {
        return List.copyOf(sent);
    }
}
