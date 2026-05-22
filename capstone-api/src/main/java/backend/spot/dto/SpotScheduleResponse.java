package backend.spot.dto;

import java.util.List;

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

	@Schema(description = "스팟 ID", example = "spot-uuid-string")
	private String spotId;

	@Builder.Default
	@Schema(description = "제안된 일정 후보 슬롯")
	private List<ScheduleSlotDto> proposedSlots = List.of();

	@Schema(description = "확정 슬롯 (없으면 null)", nullable = true)
	private ScheduleSlotDto confirmedSlot;
}
