package backend.spot.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "스팟 일정 등록/수정 요청 DTO")
public class CreateScheduleRequest {

	@NotBlank
	@Schema(description = "일정 제목", example = "한강 만남")
	private String title;

	@NotNull
	@Schema(description = "예정 일시", example = "2026-05-01T14:00:00")
	private LocalDateTime scheduledAt;
}
