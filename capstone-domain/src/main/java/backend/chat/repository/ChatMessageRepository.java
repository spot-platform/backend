package backend.chat.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import backend.chat.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	List<ChatMessage> findByChatRoomId(Long chatRoomId);

	List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);

	List<ChatMessage> findByChatRoomIdOrderByIdDesc(Long chatRoomId, Pageable pageable);

	Optional<ChatMessage> findTopByChatRoomIdOrderByIdDesc(Long chatRoomId);

	@Query("""
		select m
		from ChatMessage m
		where m.chatRoomId in :roomIds
			and m.id = (
				select max(m2.id)
				from ChatMessage m2
				where m2.chatRoomId = m.chatRoomId
			)
		""")
	List<ChatMessage> findLatestByChatRoomIds(@Param("roomIds") Collection<Long> roomIds);

	@Query("select distinct m.chatRoomId from ChatMessage m where m.senderId = :userId")
	List<Long> findDistinctChatRoomIdsBySenderId(@Param("userId") String userId);

	List<ChatMessage> findByChatRoomIdAndIdLessThanOrderByIdDesc(Long chatRoomId, Long id, Pageable pageable);

	/**
	 * 본인 멤버십의 lastReadMessageId 기준 unread 카운트를 방별로 묶어 반환.
	 * 룸 ID 마다 ChatMessageRepository 를 따로 호출하지 않기 위한 N+1 가드 쿼리.
	 *
	 * <p>{@link backend.chat.entity.ChatRoomMember} 조인으로 본인 멤버십이 없는 방은 자동 제외된다.
	 * lastReadMessageId 가 null 인 멤버는 0 으로 간주하지 않고 모든 메시지를 unread 로 친다.
	 */
	@Query("""
		select m.chatRoomId, count(m.id)
		from ChatMessage m, ChatRoomMember mb
		where mb.userId = :userId
			and mb.chatRoomId = m.chatRoomId
			and m.chatRoomId in :roomIds
			and m.id > coalesce(mb.lastReadMessageId, 0)
		group by m.chatRoomId
		""")
	List<Object[]> countUnreadByUserAndRoomIds(
		@Param("userId") String userId,
		@Param("roomIds") Collection<Long> roomIds
	);
}
