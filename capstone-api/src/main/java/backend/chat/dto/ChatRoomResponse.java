package backend.chat.dto;

import java.time.LocalDateTime;

import backend.chat.entity.ChatRoom;
import backend.chat.entity.ChatRoomType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "채팅방 응답 DTO")
public class ChatRoomResponse {

	@Schema(description = "채팅방 ID", example = "1")
	private Long id;

	@Schema(description = "연결된 스팟 ID (그룹 채팅인 경우)", example = "550e8400-e29b-41d4-a716-446655440000")
	private String spotId;

	@Schema(description = "채팅방 타입 (GROUP / PERSONAL)", example = "GROUP")
	private ChatRoomType type;

	@Schema(description = "생성 일시")
	private LocalDateTime createdAt;

	public static ChatRoomResponse from(ChatRoom room) {
		return ChatRoomResponse.builder()
			.id(room.getId())
			.spotId(room.getSpotId())
			.type(room.getType())
			.createdAt(room.getCreatedAt())
			.build();
	}
}
