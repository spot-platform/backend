package backend.chat.dto;

import backend.chat.entity.ChatRoomType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "채팅방 생성 요청 DTO")
public class CreateChatRoomRequest {

	@Schema(description = "연결할 스팟 ID (그룹 채팅인 경우, 개인 채팅은 null)", example = "550e8400-e29b-41d4-a716-446655440000")
	private String spotId;

	@NotNull
	@Schema(description = "채팅방 타입 (GROUP / PERSONAL)", example = "GROUP")
	private ChatRoomType type;
}
