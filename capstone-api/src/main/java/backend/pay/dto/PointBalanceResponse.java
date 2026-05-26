package backend.pay.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PointBalanceResponse(
	@Schema(description = "현재 포인트 잔액", example = "42000")
	Long balance,

	@Schema(description = "최종 갱신 시각 (ISO 8601)", example = "2026-05-23T16:00:00")
	String updatedAt
) {
}
