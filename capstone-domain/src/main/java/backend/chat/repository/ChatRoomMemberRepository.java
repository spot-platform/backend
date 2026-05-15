package backend.chat.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import backend.chat.entity.ChatRoomMember;

public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

	boolean existsByChatRoomIdAndUserId(Long chatRoomId, String userId);

	Optional<ChatRoomMember> findByChatRoomIdAndUserId(Long chatRoomId, String userId);

	List<ChatRoomMember> findByChatRoomId(Long chatRoomId);

	List<ChatRoomMember> findByUserId(String userId);

	@Query("select m.chatRoomId from ChatRoomMember m where m.userId = :userId")
	List<Long> findChatRoomIdsByUserId(@Param("userId") String userId);

	@Query("select m.userId from ChatRoomMember m where m.chatRoomId = :chatRoomId")
	List<String> findUserIdsByChatRoomId(@Param("chatRoomId") Long chatRoomId);

	/**
	 * 두 유저가 정확히 둘만 멤버로 있는 PERSONAL 방 ID 를 조회.
	 * PERSONAL 재사용 정책 (A↔B 채팅 다시 시작 시 기존 방 반환) 의 핵심 쿼리.
	 */
	@Query("""
		select m.chatRoomId
		from ChatRoomMember m
		where m.chatRoomId in (
			select m2.chatRoomId from ChatRoomMember m2
			where m2.userId in (:userA, :userB)
			group by m2.chatRoomId
			having count(distinct m2.userId) = 2
		)
		group by m.chatRoomId
		having count(m.userId) = 2
		""")
	List<Long> findPersonalRoomIdsBetween(@Param("userA") String userA, @Param("userB") String userB);

	long countByChatRoomId(Long chatRoomId);

	@Query("select m.chatRoomId, count(m) from ChatRoomMember m where m.chatRoomId in :roomIds group by m.chatRoomId")
	List<Object[]> countMembersGroupedByRoomIds(@Param("roomIds") Collection<Long> roomIds);
}
