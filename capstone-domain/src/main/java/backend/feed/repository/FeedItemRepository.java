package backend.feed.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import backend.feed.entity.FeedItem;
import jakarta.persistence.LockModeType;

@Repository
public interface FeedItemRepository extends JpaRepository<FeedItem, Long>,
		JpaSpecificationExecutor<FeedItem>,
		FeedItemRepositoryCustom {

	Optional<FeedItem> findByIdAndDeletedFalse(Long id);

	/**
	 * 펀딩 달성 시 Spot 전환 처리에서 동시 수락 경합을 방지하기 위한 비관적 락 조회.
	 * acceptApplication() 전용 — 일반 조회에는 findByIdAndDeletedFalse() 사용.
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT f FROM FeedItem f WHERE f.id = :id AND f.deleted = false")
	Optional<FeedItem> findByIdAndDeletedFalseForUpdate(Long id);
}
