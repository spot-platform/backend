package backend.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "채팅방 알림 설정 변경 요청 DTO")
public class UpdateChatNotificationRequest {

	@NotNull
	@Schema(description = "알림 수신 여부 (false = 음소거)", example = "false")
	private Boolean enabled;
}
