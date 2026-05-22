package backend.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "투표 참여 요청 DTO (단건 토글)")
public class CastChatVoteRequest {

	@NotNull
	@Schema(description = "선택한 선택지 ID", example = "1")
	private Long optionId;
}
