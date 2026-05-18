package backend.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.chat.entity.ChatBlock;

public interface ChatBlockRepository extends JpaRepository<ChatBlock, Long> {

	/** 본인이 차단한 목록 — 최신순 */
	List<ChatBlock> findByBlockerIdOrderByCreatedAtDesc(String blockerId);

	Optional<ChatBlock> findByBlockerIdAndBlockedId(String blockerId, String blockedId);

	boolean existsByBlockerIdAndBlockedId(String blockerId, String blockedId);

	/**
	 * (a ↔ b) 사이에 어느 쪽이든 차단이 존재하는지 — PERSONAL 방 시작 시 양방향 검증용.
	 */
	default boolean existsBetween(String userA, String userB) {
		return existsByBlockerIdAndBlockedId(userA, userB)
			|| existsByBlockerIdAndBlockedId(userB, userA);
	}

	long deleteByBlockerIdAndBlockedId(String blockerId, String blockedId);
}
