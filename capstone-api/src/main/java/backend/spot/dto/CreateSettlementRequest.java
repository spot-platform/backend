package backend.spot.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "스팟 정산 요청 DTO")
public class CreateSettlementRequest {

	@NotEmpty
	@Valid
	@Schema(description = "정산 항목 목록")
	private List<SettlementLineItemDto> lineItems;

	@NotBlank
	@Schema(description = "정산 요약", example = "재료비 + 대관료 정산")
	private String summary;
}
