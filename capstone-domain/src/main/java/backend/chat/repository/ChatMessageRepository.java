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
}
