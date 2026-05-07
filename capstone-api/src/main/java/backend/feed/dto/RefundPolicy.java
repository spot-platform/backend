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
@Schema(description = "환불 정책 DTO")
public class RefundPolicy {

	@JsonProperty("cutoff_hours")
	@Schema(description = "환불 마감 시간", example = "24")
	private int cutoffHours;

	@JsonProperty("full_refund_until")
	@Schema(description = "전액 환불 가능 시점")
	private String fullRefundUntil;

	@Schema(description = "비고")
	private String note;
}

