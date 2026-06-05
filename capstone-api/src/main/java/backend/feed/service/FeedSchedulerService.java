package backend.feed.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

				feed.markDeadlineNotifySent();
			} catch (Exception e) {
				log.error("[FeedScheduler] 마감 임박 알림 실패 - feedId={}, error={}", feed.getId(), e.getMessage());
			}
		}
	}
}
