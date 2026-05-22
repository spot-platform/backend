package backend.notification.dto;

import java.time.LocalDateTime;

import backend.notification.entity.Notification;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationResponse {

	private Long id;
	private String message;
	private Boolean isRead;
	private LocalDateTime createdAt;

	public static NotificationResponse from(Notification notification) {
		return NotificationResponse.builder()
			.id(notification.getId())
			.message(notification.getMessage())
			.isRead(notification.getIsRead())
			.createdAt(notification.getCreatedAt())
			.build();
	}
}
