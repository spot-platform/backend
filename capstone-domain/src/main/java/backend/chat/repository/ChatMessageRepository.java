package backend.chat.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import backend.chat.entity.ChatMessage;
import backend.chat.entity.ChatMessageType;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	List<ChatMessage> findByChatRoomId(Long chatRoomId);

	List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);

	/** 사진 모인 탭 — 특정 타입(IMAGE) 메시지를 최신순으로 조회. */
	List<ChatMessage> findByChatRoomIdAndTypeOrderByIdDesc(Long chatRoomId, ChatMessageType type);

	/** 메시지 검색 — 방 안에서 내용(content)에 키워드가 포함된 메시지를 최신순으로 조회. SYSTEM 메시지는 제외. */
	List<ChatMessage> findByChatRoomIdAndTypeNotAndContentContainingIgnoreCaseOrderByIdDesc(
		Long chatRoomId, ChatMessageType type, String content, Pageable pageable);

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
	 *
	 * <p>본인이 보낸 메시지는 unread 에서 제외한다 — 메시지를 보낸 직후 본인 방의
	 * unreadCount 가 1 로 뜨는 카운트 오작동 방지. read 마커는 클라이언트가 read 엔드포인트를
	 * 호출할 때만 advance 하지만, 그 사이에도 본인 메시지가 자기 자신에게 unread 로 잡혀선 안 됨.
	 *
	 * <p>차단 필터: PERSONAL 방에 한해, 차단된 발신자의 메시지는 차단 시점과 무관하게 모두 unread 카운트에서 제외한다.
	 * GROUP 방은 차단 정책 외 — 차단 row 가 있어도 메시지는 정상 카운트된다.
	 */
	@Query("""
		select m.chatRoomId, count(m.id)
		from ChatMessage m, ChatRoomMember mb, ChatRoom r
		where mb.userId = :userId
			and mb.chatRoomId = m.chatRoomId
			and r.id = m.chatRoomId
			and m.chatRoomId in :roomIds
			and m.id > coalesce(mb.lastReadMessageId, 0)
			and m.senderId <> :userId
			and not (
				r.type = backend.chat.entity.ChatRoomType.PERSONAL
				and exists (
					select 1 from ChatBlock b
					where b.blockerId = :userId
						and b.blockedId = m.senderId
				)
			)
		group by m.chatRoomId
		""")
	List<Object[]> countUnreadByUserAndRoomIds(
		@Param("userId") String userId,
		@Param("roomIds") Collection<Long> roomIds
	);
}
