package backend.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "1:1 채팅방 생성 요청 DTO")
public class CreatePersonalChatRoomRequest {

	@NotBlank
	@Schema(description = "상대방 유저 ID", example = "user-uuid-string")
	private String partnerId;
}
