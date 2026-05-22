package backend.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.chat.entity.ChatRoom;
import backend.chat.entity.ChatRoomType;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

	List<ChatRoom> findBySpotId(String spotId);

	List<ChatRoom> findBySpotIdAndType(String spotId, ChatRoomType type);

	/**
	 * 스팟 기반 GROUP 채팅방 중 유효한(미삭제) 단건. partial unique index 와 의미적으로 짝.
	 * DB 레벨 제약 (uq_chat_room_group_spot) 으로 1 개 이하가 보장됨.
	 */
	Optional<ChatRoom> findFirstBySpotIdAndTypeAndIsDeletedFalse(String spotId, ChatRoomType type);

	/** Feed 단계(postId 기준)의 GROUP 방 조회. Feed → Spot 전환 시 기존 방을 재사용하기 위해 사용. */
	Optional<ChatRoom> findFirstByPostIdAndTypeAndIsDeletedFalse(String postId, ChatRoomType type);
}
