package backend.spot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "체크리스트 담당자 지정/해제 요청 DTO")
public class AssignChecklistRequest {

	@Schema(description = "담당자 user id. null 이면 담당자 해제", nullable = true, example = "user-uuid-string")
	private String assigneeId;
}
