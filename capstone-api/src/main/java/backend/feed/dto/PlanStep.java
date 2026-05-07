package backend.feed.dto;

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
@Schema(description = "계획 단계 DTO")
public class PlanStep {

	@Schema(description = "시간", example = "10:00")
	private String time;

	@Schema(description = "활동", example = "집결")
	private String activity;

	@JsonProperty("place_id")
	@Schema(description = "장소 ID")
	private String placeId;

	@Schema(description = "의도")
	private String intent;
}

