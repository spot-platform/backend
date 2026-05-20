package backend.spot.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "스팟 일정 저장 요청 DTO (전체 교체)")
public class UpdateScheduleRequest {

	@NotNull
	@Valid
	@Schema(description = "제안 슬롯 목록 (전체 교체)")
	private List<ScheduleSlotDto> proposedSlots;

	@Valid
	@Schema(description = "확정 슬롯 (proposedSlots 중 하나여야 함, 없으면 null)", nullable = true)
	private ScheduleSlotDto confirmedSlot;
}
