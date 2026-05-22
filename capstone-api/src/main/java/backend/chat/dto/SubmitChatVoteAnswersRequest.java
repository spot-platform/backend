package backend.chat.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "투표 일괄 제출 요청 DTO (최종 확정 상태 전달, 빈 배열 = 전체 취소)")
public class SubmitChatVoteAnswersRequest {

	@NotNull
	@Schema(description = "최종 선택한 선택지 ID 목록 (빈 배열이면 투표 전체 취소)", example = "[1, 3]")
	private List<Long> optionIds;
}
