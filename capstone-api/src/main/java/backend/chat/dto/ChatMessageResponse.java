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

	@Schema(description = "발신자 ID. SYSTEM 메시지는 \"SYSTEM\" 고정값", example = "user-uuid-string")
	private String senderId;

	@Schema(description = "메시지 분류 (USER / SYSTEM). SYSTEM 은 \"OO 님이 나갔습니다\" 등 서버 생성 안내", example = "USER")
	private ChatMessageType type;

	@Schema(description = "메시지 내용. 차단된 발신자의 메시지는 placeholder 로 대체됨", example = "안녕하세요")
	private String content;

	@Schema(description = "본 메시지가 차단된 발신자의 메시지인지 여부. true 면 content 는 placeholder", example = "false")
	private Boolean blocked;

	@Schema(description = "전송 일시")
	private LocalDateTime createdAt;

	private static final String BLOCKED_PLACEHOLDER = "차단한 사용자의 메시지입니다.";

	public static ChatMessageResponse from(ChatMessage message) {
		return from(message, false);
	}

	/**
	 * 차단 여부를 반영해 응답을 빌드. blocked=true 면 content 를 placeholder 로 갈아치우고
	 * {@code blocked} 플래그를 true 로 설정한다. senderId/type/createdAt 은 원본 그대로 유지하여
	 * 클라이언트가 흐름 (시간 위치, 누가 보냈는지) 을 끊지 않고 렌더할 수 있도록 한다.
	 */
	public static ChatMessageResponse from(ChatMessage message, boolean blocked) {
		return ChatMessageResponse.builder()
			.id(message.getId())
			.chatRoomId(message.getChatRoomId())
			.senderId(message.getSenderId())
			.type(message.getType())
			.content(blocked ? BLOCKED_PLACEHOLDER : message.getContent())
			.blocked(blocked)
			.createdAt(message.getCreatedAt())
			.build();
	}
}
