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

	/**
	 * 멤버 row 를 hard-delete. 나가기 흐름에서 사용.
	 * 반환값은 영향받은 row 수 — 0 이면 멤버 아니었음 (이미 나간 후 재요청 등).
	 */
	long deleteByChatRoomIdAndUserId(Long chatRoomId, String userId);

	Optional<ChatRoomMember> findByChatRoomIdAndUserId(Long chatRoomId, String userId);

	List<ChatRoomMember> findByChatRoomId(Long chatRoomId);

	List<ChatRoomMember> findByUserId(String userId);

	@Query("select m.chatRoomId from ChatRoomMember m where m.userId = :userId")
	List<Long> findChatRoomIdsByUserId(@Param("userId") String userId);

	@Query("select m.userId from ChatRoomMember m where m.chatRoomId = :chatRoomId")
	List<String> findUserIdsByChatRoomId(@Param("chatRoomId") Long chatRoomId);

	/**
	 * 두 유저가 정확히 둘만 멤버로 있는 PERSONAL 활성 방 ID 를 조회.
	 * PERSONAL 재사용 정책 (A↔B 채팅 다시 시작 시 기존 방 반환) 의 핵심 쿼리.
	 *
	 * <p>ChatRoom 의 type/isDeleted 도 함께 필터하여, GROUP 방이나 soft-deleted 방이
	 * 우연히 동일한 두 유저만 멤버로 가지더라도 잘못 재사용되지 않도록 한다.
	 */
	@Query("""
		select m.chatRoomId
		from ChatRoomMember m
		where exists (
			select 1 from ChatRoom r
			where r.id = m.chatRoomId
				and r.type = backend.chat.entity.ChatRoomType.PERSONAL
				and r.isDeleted = false
		)
		and m.chatRoomId in (
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
