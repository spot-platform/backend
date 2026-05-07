package backend.feed.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

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
@Schema(description = "계획 V3 DTO")
public class PlanV3 {

	@Schema(description = "계획 단계 목록")
	private List<PlanStep> steps;

	@JsonProperty("total_duration_minutes")
	@Schema(description = "총 소요 시간(분)", example = "120")
	private int totalDurationMinutes;
}

