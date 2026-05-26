package backend.pay.dto;

import java.time.format.DateTimeFormatter;

import backend.pay.entity.PointTransaction;
import backend.pay.entity.PointTransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record PointTransactionResponse(
	@Schema(description = "거래 ID", example = "1")
	Long id,

	@Schema(description = "거래 유형", example = "CHARGE")
	PointTransactionType type,

	@Schema(description = "거래 금액", example = "5000")
	Long amount,

	@Schema(description = "거래 후 잔액", example = "47000")
	Long balanceAfter,

	@Schema(description = "거래 설명", example = "포인트 충전")
	String description,

	@Schema(description = "거래 일시 (ISO 8601)", example = "2026-05-23T16:00:00")
	String createdAt
) {
	public static PointTransactionResponse from(PointTransaction tx) {
		return PointTransactionResponse.builder()
			.id(tx.getId())
			.type(tx.getType())
			.amount(tx.getAmount())
			.balanceAfter(tx.getBalanceAfter())
			.description(tx.getDescription())
			.createdAt(tx.getCreatedAt() != null
				? tx.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
				: null)
			.build();
	}
}
