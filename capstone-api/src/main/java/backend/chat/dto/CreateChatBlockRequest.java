package backend.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "유저 차단 요청 DTO")
public class CreateChatBlockRequest {

	@NotBlank
	@Schema(description = "차단할 유저 ID", example = "user-uuid-string")
	private String userId;
}
