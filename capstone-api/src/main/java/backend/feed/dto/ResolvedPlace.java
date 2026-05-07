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
@Schema(description = "해결된 장소 DTO")
public class ResolvedPlace {

	@JsonProperty("place_id")
	@Schema(description = "장소 ID", example = "place-001")
	private String placeId;

	@Schema(description = "장소명", example = "카페")
	private String name;

	@JsonProperty("primary_category")
	@Schema(description = "주요 카테고리", example = "카페")
	private String primaryCategory;

	@Schema(description = "장소 역할", example = "main")
	private String role;

	@Schema(description = "위도", example = "37.5665")
	private double lat;

	@Schema(description = "경도", example = "126.9780")
	private double lng;

	@Schema(description = "주소")
	private String address;

	@JsonProperty("road_address")
	@Schema(description = "도로명 주소")
	private String roadAddress;

	@Schema(description = "신뢰도", example = "0.92")
	private double confidence;
}

