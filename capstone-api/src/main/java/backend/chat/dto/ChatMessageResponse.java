package backend.chat.dto;

import java.time.LocalDateTime;

import backend.chat.entity.ChatMessage;
import backend.chat.entity.ChatMessageType;
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

	@Schema(description = "메시지 종류. 'message' (일반) | 'system' (입퇴장 안내 등)", example = "message")
	private String kind;

	@Schema(description = "발신자 ID. system 메시지는 null", example = "user-uuid-string")
	private String authorId;

	@Schema(description = "발신자 닉네임. system 메시지는 null", example = "김동현")
	private String authorName;

	@Schema(description = "메시지 내용. 차단된 발신자의 메시지는 placeholder 로 대체됨", example = "안녕하세요")
	private String content;

	@Schema(description = "본 메시지가 차단된 발신자의 메시지인지 여부. true 면 content 는 placeholder", example = "false")
	private Boolean blocked;

	@Schema(description = "전송 일시")
	private LocalDateTime createdAt;

	private static final String BLOCKED_PLACEHOLDER = "차단한 사용자의 메시지입니다.";

	public static ChatMessageResponse from(ChatMessage message) {
		return from(message, null, false);
	}

	public static ChatMessageResponse from(ChatMessage message, boolean blocked) {
		return from(message, null, blocked);
	}

	/**
	 * authorName 과 차단 여부를 반영해 응답을 빌드.
	 * blocked=true 면 content 를 placeholder 로 교체.
	 * SYSTEM 메시지는 authorId / authorName null, kind = "system".
	 */
	public static ChatMessageResponse from(ChatMessage message, String authorName, boolean blocked) {
		boolean isSystem = message.getType() == ChatMessageType.SYSTEM;
		return ChatMessageResponse.builder()
			.id(message.getId())
			.chatRoomId(message.getChatRoomId())
			.kind(isSystem ? "system" : "message")
			.authorId(isSystem ? null : message.getSenderId())
			.authorName(isSystem ? null : authorName)
			.content(blocked ? BLOCKED_PLACEHOLDER : message.getContent())
			.blocked(blocked)
			.createdAt(message.getCreatedAt())
			.build();
	}
}
