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
		return builder()
			.type(ChatSseEventType.READ)
			.data(roomUserPayload(roomId, userId))
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
