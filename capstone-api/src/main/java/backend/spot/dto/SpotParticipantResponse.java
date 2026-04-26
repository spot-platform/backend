package backend.spot.dto;

import java.time.LocalDateTime;

import backend.spot.entity.ParticipantRole;
import backend.spot.entity.ParticipantState;
import backend.spot.entity.SpotParticipant;
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
@Schema(description = "스팟 참여자 응답 DTO")
public class SpotParticipantResponse {

	@Schema(description = "참여자 레코드 ID", example = "1")
	private Long id;

	@Schema(description = "유저 ID", example = "user-uuid-string")
	private String userId;

	@Schema(description = "역할 (AUTHOR: 작성자, PARTICIPANT: 참여자)", example = "PARTICIPANT")
	private ParticipantRole role;

	@Schema(description = "참여 상태 (ACTIVE / LEFT / EXPELLED)", example = "ACTIVE")
	private ParticipantState state;

	@Schema(description = "참여 일시")
	private LocalDateTime joinedAt;

	public static SpotParticipantResponse from(SpotParticipant participant) {
		return SpotParticipantResponse.builder()
			.id(participant.getId())
			.userId(participant.getUserId())
			.role(participant.getRole())
			.state(participant.getState())
			.joinedAt(participant.getJoinedAt())
			.build();
	}
}
