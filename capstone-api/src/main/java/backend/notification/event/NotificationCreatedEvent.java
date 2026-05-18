package backend.notification.event;

import backend.notification.dto.NotificationResponse;

public record NotificationCreatedEvent(String userId, NotificationResponse notification) {
}
