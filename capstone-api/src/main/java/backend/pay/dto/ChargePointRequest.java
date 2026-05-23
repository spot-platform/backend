package backend.pay.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ChargePointRequest(
	@Schema(description = "충전 금액 (최소 1000)", example = "5000")
	@NotNull(message = "충전 금액은 필수입니다.")
	@Min(value = 1000, message = "최소 충전 금액은 1000 포인트입니다.")
	Long amount
) {
}
