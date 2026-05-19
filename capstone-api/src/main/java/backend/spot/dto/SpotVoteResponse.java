package backend.spot.dto;

import java.time.LocalDateTime;
import java.util.List;

import backend.spot.entity.SpotVote;
import backend.spot.entity.VoteState;
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
@Schema(description = "스팟 투표 응답 DTO")
public class SpotVoteResponse {

	@Schema(description = "투표 ID", example = "1")
	private Long id;

	@Schema(description = "스팟 ID", example = "spot-uuid-string")
	private String spotId;

	@Schema(description = "질문", example = "어디서 만날까요?")
	private String question;

	@Schema(description = "투표 상태 (ACTIVE / CLOSED)", example = "ACTIVE")
	private VoteState state;

	@Schema(description = "생성자 ID", example = "user-uuid-string")
	private String creatorId;

	@Schema(description = "선택지 목록")
	private List<SpotVoteOptionResponse> options;

	@Schema(description = "다중 선택 가능 여부", example = "false")
	private boolean multiSelect;

	@Schema(description = "투표 마감 일시", nullable = true)
	private LocalDateTime closedAt;

	@Schema(description = "인증 사용자가 선택한 옵션 ID 목록 (비인증 시 null)", nullable = true)
	private List<Long> myVotedOptionIds;

	@Schema(description = "생성 일시")
	private LocalDateTime createdAt;

	public static SpotVoteResponse of(SpotVote vote, List<SpotVoteOptionResponse> options) {
		return of(vote, options, null, vote.getSpotId());
	}

	public static SpotVoteResponse of(SpotVote vote, List<SpotVoteOptionResponse> options, List<Long> myVotedOptionIds) {
		return of(vote, options, myVotedOptionIds, vote.getSpotId());
	}

	public static SpotVoteResponse of(
		SpotVote vote,
		List<SpotVoteOptionResponse> options,
		List<Long> myVotedOptionIds,
		String spotId
	) {
		return SpotVoteResponse.builder()
			.id(vote.getId())
			.spotId(spotId)
			.question(vote.getQuestion())
			.state(vote.getState())
			.creatorId(vote.getCreatorId())
			.options(options)
			.multiSelect(vote.isMultiSelect())
			.closedAt(vote.getClosedAt())
			.myVotedOptionIds(myVotedOptionIds)
			.createdAt(vote.getCreatedAt())
			.build();
	}
}
