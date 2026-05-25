package backend.spot.dto;

import java.time.LocalDateTime;
import java.util.List;

import backend.spot.entity.SpotSettlement;
import backend.spot.entity.WorkflowApprovalStatus;
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
@Schema(description = "스팟 정산 승인 응답 DTO")
public class SpotSettlementResponse {

	@Schema(description = "정산 ID", example = "1")
	private Long id;

	@Schema(description = "스팟 ID", example = "1")
	private Long spotId;

	@Schema(description = "정산 상태 (PENDING: 승인 대기, APPROVED: 승인 완료)", example = "PENDING")
	private WorkflowApprovalStatus status;

	@Schema(description = "정산 요약", example = "재료비 + 대관료 정산")
	private String summary;

	@Schema(description = "정산 총액 (원)", example = "30000")
	private Integer totalAmount;

	@Schema(description = "정산 항목 목록")
	private List<SettlementLineItemDto> lineItems;

	@Schema(description = "요청자 ID", example = "user-uuid-string")
	private String requesterId;

	@Schema(description = "요청 일시", example = "2024-04-10T12:00:00")
	private LocalDateTime createdAt;

	@Schema(description = "승인 일시", nullable = true, example = "2024-04-10T13:00:00")
	private LocalDateTime approvedAt;

	public static SpotSettlementResponse of(SpotSettlement settlement, List<SettlementLineItemDto> lineItems) {
		return SpotSettlementResponse.builder()
			.id(settlement.getId())
			.spotId(settlement.getSpotId())
			.status(settlement.getStatus())
			.summary(settlement.getSummary())
			.totalAmount(settlement.getTotalAmount())
			.lineItems(lineItems)
			.requesterId(settlement.getRequesterId())
			.createdAt(settlement.getCreatedAt())
			.approvedAt(settlement.getApprovedAt())
			.build();
	}
}
