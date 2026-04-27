package backend.spot.dto;

import java.time.LocalDateTime;

import backend.spot.entity.SpotChecklist;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "스팟 체크리스트 항목 응답 DTO")
public class SpotChecklistResponse {

	@Schema(description = "체크리스트 항목 ID", example = "1")
	private Long id;

	@Schema(description = "항목 내용", example = "돗자리 준비")
	private String content;

	@Schema(description = "완료 여부", example = "false")
	private Boolean isDone;

	@Schema(description = "등록 일시")
	private LocalDateTime createdAt;

	public static SpotChecklistResponse from(SpotChecklist checklist) {
		return SpotChecklistResponse.builder()
			.id(checklist.getId())
			.content(checklist.getContent())
			.isDone(checklist.getIsDone())
			.createdAt(checklist.getCreatedAt())
			.build();
	}
}
