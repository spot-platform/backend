package backend.notification.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import backend.global.error.exception.BusinessException;
import backend.global.error.exception.ErrorCode;
import backend.notification.dto.NotificationResponse;
import backend.notification.entity.Notification;
import backend.notification.event.NotificationCreatedEvent;
import backend.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
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

	public void markAsRead(String userId, Long notificationId) {
		Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));
		notification.markAsRead();
	}

	public void markAllAsRead(String userId) {
		notificationRepository.bulkMarkAllAsRead(userId);
	}

	/**
	 * 외부 트랜잭션이 활성화되어 있으면 커밋 후에만 알림 발송.
	 * 트랜잭션이 없으면 즉시 발송.
	 */
	public void sendAfterCommit(String userId, String message) {
		if (TransactionSynchronizationManager.isActualTransactionActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					try {
						send(userId, message);
					} catch (Exception e) {
						log.warn("[notification] afterCommit 알림 전송 실패 - userId={}, error={}", userId, e.getMessage());
					}
				}
			});
		} else {
			try {
				send(userId, message);
			} catch (Exception e) {
				log.warn("[notification] 알림 전송 실패 - userId={}, error={}", userId, e.getMessage());
			}
		}
	}
}
