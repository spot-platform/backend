package backend.feed.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import backend.feed.repository.FeedItemRepository;
import lombok.RequiredArgsConstructor;

/**
 * 피드 마감 임박 알림 플래그를 별도 트랜잭션으로 갱신하는 서비스.
 *
 * <p>{@link FeedSchedulerService}에서 self-invocation 으로 호출하면 Spring AOP proxy 를 우회해
 * {@code @Transactional(REQUIRES_NEW)} 가 적용되지 않는 문제를 방지하기 위해 별도 빈으로 분리.
 */
@Service
@RequiredArgsConstructor
public class FeedDeadlineFlagService {

	private final FeedItemRepository feedItemRepository;

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void markDeadlineNotifySent(Long feedId) {
		feedItemRepository.findById(feedId).ifPresent(f -> {
			f.markDeadlineNotifySent();
			feedItemRepository.save(f);
		});
	}
}
