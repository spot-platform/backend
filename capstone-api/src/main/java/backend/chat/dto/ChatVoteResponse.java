package backend.chat.dto;

import java.time.LocalDateTime;
import java.util.List;

import backend.chat.entity.ChatVote;
import backend.chat.entity.ChatVoteState;
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
@Schema(description = "채팅 투표 응답 DTO")
public class ChatVoteResponse {

	@Schema(description = "투표 ID", example = "1")
	private Long id;

	@Schema(description = "채팅방 ID", example = "5")
	private Long chatRoomId;

	@Schema(description = "투표 생성자 ID")
	private String creatorId;

	@Schema(description = "투표 질문", example = "어디서 만날까요?")
	private String question;

	@Schema(description = "투표 상태", example = "ACTIVE")
	private ChatVoteState state;

	@Schema(description = "다중선택 허용 여부", example = "false")
	private boolean multiSelect;

	@Schema(description = "익명 투표 여부", example = "false")
	private boolean anonymous;

	@Schema(description = "마감 일시 (null = 수동 마감)")
	private LocalDateTime closedAt;

	@Schema(description = "생성 일시")
	private LocalDateTime createdAt;

	@Schema(description = "선택지 목록")
	private List<ChatVoteOptionResponse> options;

	@Schema(description = "내가 투표한 선택지 ID 목록")
	private List<Long> myVotedOptionIds;

	@Schema(description = "총 투표 참여자 수")
	private int totalVoterCount;

	public static ChatVoteResponse of(
		ChatVote vote,
		List<ChatVoteOptionResponse> options,
		List<Long> myVotedOptionIds,
		int totalVoterCount
	) {
		return ChatVoteResponse.builder()
			.id(vote.getId())
			.chatRoomId(vote.getChatRoomId())
			.creatorId(vote.getCreatorId())
			.question(vote.getQuestion())
			.state(vote.getState())
			.multiSelect(vote.isMultiSelect())
			.anonymous(vote.isAnonymous())
			.closedAt(vote.getClosedAt())
			.createdAt(vote.getCreatedAt())
			.options(options)
			.myVotedOptionIds(myVotedOptionIds)
			.totalVoterCount(totalVoterCount)
			.build();
	}
}
