package backend.spot.dto;

import backend.global.enums.FeedItemStatus;
import backend.global.enums.FeedType;
import backend.spot.entity.Spot;
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
@Schema(description = "지도 마커용 경량 스팟 응답 DTO")
public class SpotMapItemResponse {

	@Schema(description = "스팟 ID", example = "spot-uuid-string")
	private String id;

	@Schema(description = "스팟 타입", example = "OFFER")
	private FeedType type;

	@Schema(description = "스팟 상태", example = "OPEN")
	private FeedItemStatus status;

	@Schema(description = "제목", example = "한강 자리 제공합니다")
	private String title;

	@Schema(description = "좌표")
	private Coord coord;

	@Schema(description = "카테고리", example = "요리", nullable = true)
	private String category;

	@Schema(description = "작성자 ID", example = "user-uuid-string")
	private String authorId;

	@Getter
	@Builder
	@AllArgsConstructor
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Schema(description = "위경도 좌표")
	public static class Coord {
		@Schema(description = "위도", example = "37.5283")
		private Double lat;

		@Schema(description = "경도", example = "126.9294")
		private Double lng;
	}

	public static SpotMapItemResponse from(Spot spot) {
		return SpotMapItemResponse.builder()
			.id(spot.getId())
			.type(spot.getType())
			.status(spot.getStatus())
			.title(spot.getTitle())
			.coord(Coord.builder().lat(spot.getLat()).lng(spot.getLng()).build())
			.category(spot.getCategory())
			.authorId(spot.getAuthorId())
			.build();
	}
}
