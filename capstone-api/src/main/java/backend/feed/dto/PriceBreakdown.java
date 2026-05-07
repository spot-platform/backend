package backend.feed.dto;

import java.util.List;

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
@Schema(description = "가격 상세 DTO")
public class PriceBreakdown {

	@JsonProperty("base_fee")
	@Schema(description = "기본 금액", example = "5000")
	private Integer baseFee;

	@JsonProperty("included_items")
	@Schema(description = "포함 항목 목록")
	private List<IncludedItem> includedItems;

	@JsonProperty("optional_addons")
	@Schema(description = "선택 추가 옵션 목록")
	private List<AddOn> optionalAddons;

	@JsonProperty("refund_policy")
	@Schema(description = "환불 정책")
	private RefundPolicy refundPolicy;

	@JsonProperty("summary_line")
	@Schema(description = "요약 문구")
	private String summaryLine;
}

