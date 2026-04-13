package backend.chat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.chat.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	List<ChatMessage> findByChatRoomId(Long chatRoomId);

	List<ChatMessage> findByChatRoomIdOrderByCreatedAtAsc(Long chatRoomId);
}
