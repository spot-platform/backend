package backend.notification.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import backend.global.error.exception.BusinessException;
import backend.global.error.exception.ErrorCode;
import backend.notification.dto.NotificationResponse;
import backend.notification.entity.Notification;
import backend.notification.event.NotificationCreatedEvent;
import backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

	private final NotificationRepository notificationRepository;
	private final ApplicationEventPublisher eventPublisher;

	public void send(String userId, String message) {
		Notification notification = Notification.builder()
			.userId(userId)
			.message(message)
			.build();

		Notification saved = notificationRepository.save(notification);
		eventPublisher.publishEvent(new NotificationCreatedEvent(userId, NotificationResponse.from(saved)));
	}

	@Transactional(readOnly = true)
	public Page<NotificationResponse> getNotifications(String userId, Pageable pageable) {
		return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
			.map(NotificationResponse::from);
	}

	public void markAsRead(String userId, String notificationId) {
		Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
		notification.markAsRead();
	}

	public void markAllAsRead(String userId) {
		notificationRepository.bulkMarkAllAsRead(userId);
	}
}
