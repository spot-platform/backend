package backend.chat.dto;

import java.time.LocalDateTime;

import backend.chat.entity.ChatMessage;
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
@Schema(description = "채팅 메시지 응답 DTO")
public class ChatMessageResponse {

	@Schema(description = "메시지 ID (커서 기반 페이지네이션 기준값)", example = "42")
	private Long id;

	@Schema(description = "채팅방 ID", example = "1")
	private Long chatRoomId;

	@Schema(description = "발신자 ID", example = "user-uuid-string")
	private String senderId;

	@Schema(description = "메시지 내용")
	private String content;

	@Schema(description = "전송 일시")
	private LocalDateTime createdAt;

	public static ChatMessageResponse from(ChatMessage message) {
		return ChatMessageResponse.builder()
			.id(message.getId())
			.chatRoomId(message.getChatRoomId())
			.senderId(message.getSenderId())
			.content(message.getContent())
			.createdAt(message.getCreatedAt())
			.build();
	}
}
