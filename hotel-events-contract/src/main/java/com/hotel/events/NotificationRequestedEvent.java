package com.hotel.events;

import java.io.Serializable;

public record NotificationRequestedEvent(
        String recipient,
        String subject,
        String message,
        String channel // EMAIL, SMS, PUSH
) implements Serializable {}