package backend.spot.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "스팟 투표 생성 요청 DTO")
public class CreateVoteRequest {

	@NotBlank
	@Schema(description = "투표 질문", example = "어디서 만날까요?")
	private String question;

	@NotEmpty
	@Schema(description = "선택지 목록 (최소 2개)", example = "[\"여의도\", \"뚝섬\"]")
	private List<String> options;
}
