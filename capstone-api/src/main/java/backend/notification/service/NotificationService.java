package backend.notification.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import backend.notification.dto.NotificationResponse;
import backend.notification.entity.Notification;
import backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final NotificationSseService notificationSseService;

	public void send(String userId, String message) {
		Notification notification = Notification.builder()
			.userId(userId)
			.message(message)
			.build();

		Notification saved = notificationRepository.save(notification);
		notificationSseService.send(userId, NotificationResponse.from(saved));
	}

	@Transactional(readOnly = true)
	public List<NotificationResponse> getNotifications(String userId) {
		return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
			.stream()
			.map(NotificationResponse::from)
			.toList();
	}

	public void markAsRead(String notificationId) {
		Notification notification = notificationRepository.findById(notificationId)
			.orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다. id=" + notificationId));
		notification.markAsRead();
	}
}
