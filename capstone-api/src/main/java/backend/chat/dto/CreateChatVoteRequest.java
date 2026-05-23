package backend.chat.dto;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "채팅 투표 생성 요청 DTO")
public class CreateChatVoteRequest {

	@NotBlank
	@Schema(description = "투표 질문", example = "어디서 만날까요?")
	private String question;

	@NotEmpty
	@Size(min = 2, max = 10, message = "선택지는 2~10개여야 합니다.")
	@Schema(description = "선택지 목록 (최소 2개, 최대 10개)", example = "[\"여의도\", \"뚝섬\", \"합정\"]")
	private List<String> options;

	@Schema(description = "다중선택 허용 여부", example = "false", defaultValue = "false")
	private boolean multiSelect;

	@Schema(description = "익명 투표 여부", example = "false", defaultValue = "false")
	private boolean anonymous;

	@Schema(description = "자동 마감 일시 (null 이면 수동 마감)", example = "2026-06-01T18:00:00")
	private LocalDateTime closedAt;
}
