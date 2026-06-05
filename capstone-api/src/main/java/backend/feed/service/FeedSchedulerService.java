package backend.feed.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
 * <p>deadlineNotifySent 플래그는 트랜잭션 커밋 이후 afterCommit에서
 * {@code REQUIRES_NEW} 별도 트랜잭션으로 저장한다.
 * 알림 큐잉 등록이 성공한 경우에만 플래그가 설정되므로,
 * transient 실패로 인한 영구적 알림 억제를 방지한다.
 *
 * <p>NOTE: 현재 단일 인스턴스 환경을 가정한다.
 * 다중 인스턴스 배포 시 같은 피드에 대한 중복 알림이 발생할 수 있으므로,
 * 그 경우 ShedLock 또는 atomic claim 패턴 도입을 검토해야 한다.
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

				// 알림 큐잉 성공 후 커밋 직후에 플래그 저장 (REQUIRES_NEW — 트랜잭션 경계 명확화)
				final Long feedId = feed.getId();
				TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
					@Override
					public void afterCommit() {
						try {
							markDeadlineNotifySent(feedId);
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

	/**
	 * deadlineNotifySent 플래그를 별도 트랜잭션으로 저장.
	 * afterCommit 콜백에서 호출되므로 외부 트랜잭션이 이미 종료된 상태 — REQUIRES_NEW로 새 트랜잭션 생성.
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markDeadlineNotifySent(Long feedId) {
		feedItemRepository.findById(feedId).ifPresent(f -> {
			f.markDeadlineNotifySent();
			feedItemRepository.save(f);
		});
	}
}
