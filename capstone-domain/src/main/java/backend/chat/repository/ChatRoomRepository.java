package backend.chat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.chat.entity.ChatRoom;
import backend.chat.entity.ChatRoomType;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

	List<ChatRoom> findBySpotId(String spotId);

	List<ChatRoom> findBySpotIdAndType(String spotId, ChatRoomType type);
}
