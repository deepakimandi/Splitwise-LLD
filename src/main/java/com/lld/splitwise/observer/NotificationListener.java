package com.lld.splitwise.observer;

import com.lld.splitwise.model.User;

/** Tells every affected user except the one who did the action ("Rahul added 'Dinner' 900.00"). */
public class NotificationListener implements EventListener {

    private final NotificationChannel channel;

    public NotificationListener(NotificationChannel channel) {
        this.channel = channel;
    }

    @Override
    public void onEvent(SplitwiseEvent event) {
        for (User user : event.affectedUsers()) {
            if (!user.equals(event.actor())) {
                channel.send(user, event.message());
            }
        }
    }
}
