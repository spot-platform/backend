package backend.user.dto.response;

import java.time.LocalDateTime;

import backend.global.enums.FeedItemStatus;
import backend.global.enums.FeedType;
import backend.spot.entity.ParticipantRole;
import backend.spot.entity.Spot;
import backend.spot.entity.SpotParticipant;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "내가 참여 중인 스팟 항목")
public class MyParticipatingSpotResponse {

	@Schema(description = "스팟 ID")
	private Long spotId;

	@Schema(description = "스팟 제목")
	private String title;

	@Schema(description = "스팟 타입 (OFFER/REQUEST)")
	private FeedType type;

	@Schema(description = "스팟 상태")
	private FeedItemStatus status;

	@Schema(description = "내 역할 (AUTHOR/PARTICIPANT)")
	private ParticipantRole role;

	@Schema(description = "참여 일시")
	private LocalDateTime joinedAt;

	public static MyParticipatingSpotResponse of(SpotParticipant participant, Spot spot) {
		return MyParticipatingSpotResponse.builder()
			.spotId(spot.getId())
			.title(spot.getTitle())
			.type(spot.getType())
			.status(spot.getStatus())
			.role(participant.getRole())
			.joinedAt(participant.getJoinedAt())
			.build();
	}
}
