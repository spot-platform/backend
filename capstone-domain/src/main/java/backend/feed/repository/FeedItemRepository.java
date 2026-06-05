package backend.feed.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import backend.feed.entity.FeedItem;
import backend.global.enums.FeedItemStatus;
import jakarta.persistence.LockModeType;

@Repository
public interface FeedItemRepository extends JpaRepository<FeedItem, Long>,
		JpaSpecificationExecutor<FeedItem>,
		FeedItemRepositoryCustom {

	Optional<FeedItem> findByIdAndDeletedFalse(Long id);

	/** 내가 작성한 피드 목록 (삭제되지 않은 것). 최신순. */
	List<FeedItem> findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(String authorId);

	/**
	 * 펀딩 달성 시 Spot 전환 처리에서 동시 수락 경합을 방지하기 위한 비관적 락 조회.
	 * acceptApplication() 전용 — 일반 조회에는 findByIdAndDeletedFalse() 사용.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT f FROM FeedItem f WHERE f.id = :id AND f.deleted = false")
	Optional<FeedItem> findByIdAndDeletedFalseForUpdate(Long id);

	boolean existsByIsAi(boolean isAi);

	/**
	 * 마감 D-1 알림 대상 피드 조회.
	 * OPEN 상태이고, 마감일이 내일이며, 아직 마감 임박 알림을 보내지 않은 피드.
	 */
	@Query("SELECT f FROM FeedItem f WHERE f.status = :status AND f.deadline = :deadline AND f.deadlineNotifySent = false AND f.deleted = false")
	List<FeedItem> findDeadlineApproachingFeeds(
		@org.springframework.data.repository.query.Param("status") FeedItemStatus status,
		@org.springframework.data.repository.query.Param("deadline") String deadline
	);
}
