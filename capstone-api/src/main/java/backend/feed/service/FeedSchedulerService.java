package backend.feed.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import backend.feed.entity.FeedApplication;
import backend.feed.entity.FeedApplicationStatus;
import backend.feed.entity.FeedItem;
import backend.feed.repository.FeedApplicationRepository;
import backend.feed.repository.FeedItemRepository;
import backend.global.enums.FeedItemStatus;
import backend.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 피드 마감 임박(D-1) 알림을 처리하는 스케줄러.
 * 매 시간 실행되며, 내일 마감인 OPEN 피드의 작성자와 수락된 신청자에게 알림을 전송한다.
 *
 * <p>deadlineNotifySent 플래그는 트랜잭션 커밋 이후 afterCommit에서 별도 트랜잭션으로 저장한다.
 * 알림 큐잉(sendAfterCommit)이 정상 등록된 경우에만 플래그가 설정되므로,
 * transient 실패로 인한 영구적 알림 억제를 방지한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedSchedulerService {

	private final FeedItemRepository feedItemRepository;
	private final FeedApplicationRepository feedApplicationRepository;
	private final NotificationService notificationService;

	@Scheduled(fixedRate = 3_600_000) // 매 1시간
	@Transactional
	public void notifyDeadlineApproachingFeeds() {
		String tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
		List<FeedItem> feeds = feedItemRepository.findDeadlineApproachingFeeds(FeedItemStatus.OPEN, tomorrow);

		if (feeds.isEmpty()) {
			return;
		}

		log.info("[FeedScheduler] 마감 임박 알림 대상 {} 건", feeds.size());

		for (FeedItem feed : feeds) {
			try {
				String message = "'" + feed.getTitle() + "'이 내일 마감돼요";

				// 작성자 알림
				notificationService.sendAfterCommit(feed.getAuthorId(), message);

				// 수락된 신청자 알림
				feedApplicationRepository.findAllByFeedItemIdAndStatus(feed.getId(), FeedApplicationStatus.ACCEPTED)
					.stream()
					.map(FeedApplication::getUserId)
					.filter(uid -> !uid.equals(feed.getAuthorId()))
					.forEach(uid -> notificationService.sendAfterCommit(uid, message));

				// 알림 큐잉 성공 후 커밋 직후에 플래그 저장 (새 트랜잭션)
				// → sendAfterCommit 등록이 성공한 경우에만 플래그가 설정됨
				final Long feedId = feed.getId();
				TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						try {
							feedItemRepository.findById(feedId).ifPresent(f -> {
								f.markDeadlineNotifySent();
								feedItemRepository.save(f);
							});
						} catch (Exception e) {
							log.error("[FeedScheduler] deadlineNotifySent 저장 실패 - feedId={}, error={}",
								feedId, e.getMessage());
						}
					}
				});

			} catch (Exception e) {
				log.error("[FeedScheduler] 마감 임박 알림 실패 - feedId={}, error={}", feed.getId(), e.getMessage());
			}
		}
	}
}
