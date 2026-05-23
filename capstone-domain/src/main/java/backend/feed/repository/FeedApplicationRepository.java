package backend.feed.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import backend.feed.entity.FeedApplication;
import backend.feed.entity.FeedApplicationStatus;

@Repository
public interface FeedApplicationRepository extends JpaRepository<FeedApplication, String> {

	Optional<FeedApplication> findByFeedItemIdAndUserIdAndStatus(
			Long feedItemId, String userId, FeedApplicationStatus status);

	Optional<FeedApplication> findByIdAndFeedItemId(String id, Long feedItemId);

	long countByFeedItemIdAndStatus(Long feedItemId, FeedApplicationStatus status);

	List<FeedApplication> findAllByFeedItemIdAndStatus(Long feedItemId, FeedApplicationStatus status);

	List<FeedApplication> findAllByFeedItemIdAndUserId(Long feedItemId, String userId);

	List<FeedApplication> findAllByFeedItemIdInAndUserId(List<Long> feedItemIds, String userId);

	/** 내가 신청한 전체 내역 (최신순). 마이페이지 "신청한 목록" 용. */
	List<FeedApplication> findAllByUserIdOrderByCreatedAtDesc(String userId);
}

