package backend.spot.dto;

import java.time.LocalDateTime;

import backend.global.enums.FeedItemStatus;
import backend.global.enums.PostType;
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
@Schema(description = "스팟 응답 DTO")
public class SpotResponse {

	@Schema(description = "스팟 ID (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
	private String id;

	@Schema(description = "스팟 타입 (OFFER: 제공, REQUEST: 요청)", example = "OFFER")
	private PostType type;

	@Schema(description = "스팟 상태 (OPEN / MATCHED / CLOSED)", example = "OPEN")
	private FeedItemStatus status;

	@Schema(description = "스팟 제목", example = "한강 자리 제공합니다")
	private String title;

	@Schema(description = "스팟 상세 설명", example = "여의도 한강공원 명당 자리입니다.")
	private String description;

	@Schema(description = "포인트 비용", example = "5000")
	private Integer pointCost;

	@Schema(description = "작성자 ID", example = "user-uuid-string")
	private String authorId;

	@Schema(description = "작성자 닉네임", example = "한강러버")
	private String authorNickname;

	@Schema(description = "생성 일시", example = "2024-04-10T10:00:00")
	private LocalDateTime createdAt;

	@Schema(description = "수정 일시", example = "2024-04-10T12:00:00")
	private LocalDateTime updatedAt;

	/**
	 * Spot 엔티티 → SpotResponse DTO 변환 팩토리 메서드
	 * Controller/Service에서 SpotResponse.from(spot) 으로 사용
	 */
	public static SpotResponse from(Spot spot) {
		return SpotResponse.builder()
			.id(spot.getId())
			.type(spot.getType())
			.status(spot.getStatus())
			.title(spot.getTitle())
			.description(spot.getDescription())
			.pointCost(spot.getPointCost())
			.authorId(spot.getAuthorId())
			.authorNickname(spot.getAuthorNickname())
			.createdAt(spot.getCreatedAt())
			.updatedAt(spot.getUpdatedAt())
			.build();
	}
}
