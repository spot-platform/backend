package backend.spot.dto;

import java.time.LocalDateTime;
import java.util.List;

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
@Schema(description = "스팟 상세 응답 DTO (기본 정보 + 타임라인)")
public class SpotDetailResponse {

	@Schema(description = "스팟 ID", example = "1")
	private Long id;

	@Schema(description = "스팟 타입 (OFFER: 제공, REQUEST: 요청)", example = "OFFER")
	private FeedType type;

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

	@Schema(description = "활성 참여자 수", example = "3")
	private Integer participantCount;

	@Schema(description = "매칭 일시", nullable = true, example = "2024-04-10T11:00:00")
	private LocalDateTime matchedAt;

	@Schema(description = "종료 일시", nullable = true, example = "2024-04-10T12:00:00")
	private LocalDateTime closedAt;

	@Schema(description = "생성 일시", example = "2024-04-10T10:00:00")
	private LocalDateTime createdAt;

	@Schema(description = "수정 일시", example = "2024-04-10T12:00:00")
	private LocalDateTime updatedAt;

	@Schema(description = "현재 인증 사용자가 권한자(작성자 또는 참여자)인지 여부. 비인증 시 false", example = "false")
	private boolean isOwner;

	@Schema(description = "스팟 활동 타임라인 (오래된 순)")
	private List<TimelineEventResponse> timeline;

	@Schema(description = "최근 정산 요청 상태", nullable = true)
	private SpotSettlementResponse settlement;

	public static SpotDetailResponse of(
		Spot spot, int participantCount, boolean isOwner, List<TimelineEventResponse> timeline,
		SpotSettlementResponse settlement
	) {
		return SpotDetailResponse.builder()
			.id(spot.getId())
			.type(spot.getType())
			.status(spot.getStatus())
			.title(spot.getTitle())
			.description(spot.getDescription())
			.pointCost(spot.getPointCost())
			.authorId(spot.getAuthorId())
			.authorNickname(spot.getAuthorNickname())
			.participantCount(participantCount)
			.matchedAt(spot.getMatchedAt())
			.closedAt(spot.getClosedAt())
			.createdAt(spot.getCreatedAt())
			.updatedAt(spot.getUpdatedAt())
			.isOwner(isOwner)
			.timeline(timeline)
			.settlement(settlement)
			.build();
	}
}
