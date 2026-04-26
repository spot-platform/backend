package backend.spot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "투표 참여 요청 DTO")
public class CastVoteRequest {

	@NotNull
	@Schema(description = "선택한 선택지 ID", example = "1")
	private Long optionId;
}
