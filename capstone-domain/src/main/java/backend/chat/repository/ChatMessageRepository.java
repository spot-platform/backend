package backend.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import backend.chat.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	List<ChatMessage> findByChatRoomId(Long chatRoomId);

	List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);

	List<ChatMessage> findByChatRoomIdOrderByIdDesc(Long chatRoomId, Pageable pageable);

	Optional<ChatMessage> findTopByChatRoomIdOrderByIdDesc(Long chatRoomId);

	List<ChatMessage> findByChatRoomIdAndIdLessThanOrderByIdDesc(Long chatRoomId, Long id, Pageable pageable);
}
