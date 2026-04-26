package backend.spot.dto;

import java.time.LocalDateTime;

import backend.spot.entity.SpotSchedule;
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
@Schema(description = "스팟 일정 응답 DTO")
public class SpotScheduleResponse {

	@Schema(description = "일정 ID", example = "1")
	private Long id;

	@Schema(description = "일정 제목", example = "한강 만남")
	private String title;

	@Schema(description = "예정 일시")
	private LocalDateTime scheduledAt;

	@Schema(description = "등록 일시")
	private LocalDateTime createdAt;

	public static SpotScheduleResponse from(SpotSchedule schedule) {
		return SpotScheduleResponse.builder()
			.id(schedule.getId())
			.title(schedule.getTitle())
			.scheduledAt(schedule.getScheduledAt())
			.createdAt(schedule.getCreatedAt())
			.build();
	}
}
