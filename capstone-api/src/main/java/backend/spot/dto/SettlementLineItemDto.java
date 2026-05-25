package backend.spot.dto;

import backend.spot.entity.SpotSettlementLineItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "정산 항목")
public class SettlementLineItemDto {

	@NotBlank
	@Schema(description = "항목명", example = "재료비")
	private String label;

	@Min(0)
	@Schema(description = "금액 (원)", example = "15000")
	private Integer amount;

	public static SettlementLineItemDto from(SpotSettlementLineItem item) {
		return SettlementLineItemDto.builder()
			.label(item.getLabel())
			.amount(item.getAmount())
			.build();
	}
}
