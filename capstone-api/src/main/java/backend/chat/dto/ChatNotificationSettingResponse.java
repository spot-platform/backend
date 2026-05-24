package backend.chat.dto;

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
@Schema(description = "채팅방 알림 설정 응답 DTO")
public class ChatNotificationSettingResponse {

	@Schema(description = "알림 수신 여부 (false = 음소거)", example = "true")
	private boolean enabled;

	public static ChatNotificationSettingResponse of(boolean enabled) {
		return ChatNotificationSettingResponse.builder()
			.enabled(enabled)
			.build();
	}
}
