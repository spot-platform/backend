package backend.chat.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.chat.entity.ChatVote;
import backend.chat.entity.ChatVoteState;

public interface ChatVoteRepository extends JpaRepository<ChatVote, Long> {

	List<ChatVote> findByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);

	/**
	 * 30분 전 알림 발송 대상: ACTIVE 상태이며 아직 알림 미발송 + closedAt이 now~(now+30분) 사이.
	 */
	List<ChatVote> findByStateAndNotifySentFalseAndClosedAtBetween(
		ChatVoteState state,
		LocalDateTime from,
		LocalDateTime to
	);

	/**
	 * 자동 마감 대상: ACTIVE 상태이며 closedAt이 현재보다 이전.
	 */
	List<ChatVote> findByStateAndClosedAtBefore(ChatVoteState state, LocalDateTime now);
}
