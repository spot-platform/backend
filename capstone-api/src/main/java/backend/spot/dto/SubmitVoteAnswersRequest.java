package backend.spot.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "투표 답변 일괄 제출 DTO (배치 방식, 카카오톡 다중선택 투표 UX)")
public class SubmitVoteAnswersRequest {

	@NotNull
	@Schema(
		description = "확정 선택지 ID 목록. 서버가 현재 답변과 diff 하여 추가/삭제를 적용. "
			+ "빈 배열 [] 은 전체 취소를 의미. 단일선택 투표는 0~1 개만 허용.",
		example = "[3, 5]"
	)
	private List<Long> optionIds;
}
