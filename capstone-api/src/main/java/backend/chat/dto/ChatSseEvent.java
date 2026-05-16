package backend.chat.dto;

import java.util.LinkedHashMap;
import java.util.Map;

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
@Schema(description = "SSE 이벤트 envelope")
public class ChatSseEvent {

	@Schema(description = "이벤트 타입", example = "message")
	private ChatSseEventType type;

	@Schema(description = "타입별 페이로드")
	private Object data;

	public static ChatSseEvent message(ChatMessageResponse msg) {
		return builder()
			.type(ChatSseEventType.MESSAGE)
			.data(msg)
			.build();
	}

	public static ChatSseEvent read(Long roomId, String userId) {
		return read(roomId, userId, null);
	}

	/**
	 * read 이벤트. {@code lastReadMessageId} 가 있으면 페이로드에 포함되어
	 * 클라이언트가 "어디까지 읽혔는지" 정확히 알 수 있다. 기존 payload (roomId/userId) 는
	 * 그대로 보존되어 backward compatible.
	 */
	public static ChatSseEvent read(Long roomId, String userId, Long lastReadMessageId) {
		Map<String, Object> payload = roomUserPayload(roomId, userId);
		if (lastReadMessageId != null) {
			payload.put("lastReadMessageId", lastReadMessageId);
		}
		return builder()
			.type(ChatSseEventType.READ)
			.data(payload)
			.build();
	}

	public static ChatSseEvent typing(Long roomId, String userId) {
		return builder()
			.type(ChatSseEventType.TYPING)
			.data(roomUserPayload(roomId, userId))
			.build();
	}

	private static Map<String, Object> roomUserPayload(Long roomId, String userId) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("roomId", roomId);
		payload.put("userId", userId);
		return payload;
	}
}
