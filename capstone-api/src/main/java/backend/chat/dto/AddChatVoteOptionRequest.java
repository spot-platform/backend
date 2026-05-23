package backend.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "투표 선택지 추가 요청 DTO")
public class AddChatVoteOptionRequest {

	@NotBlank
	@Schema(description = "추가할 선택지 내용", example = "홍대입구")
	private String content;
}
