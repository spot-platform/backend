package backend.spot.dto;

import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "일정 슬롯 (날짜+시각+가용 사용자)")
public class ScheduleSlotDto {

	@NotNull
	@Schema(description = "날짜 (YYYY-MM-DD)", example = "2026-05-01")
	private LocalDate date;

	@NotNull
	@Min(0)
	@Max(23)
	@Schema(description = "시각 (0-23)", example = "14")
	private Integer hour;

	@Builder.Default
	@Schema(description = "이 슬롯에 가능한 사용자 ID 목록")
	private List<String> availableUserIds = List.of();
}
