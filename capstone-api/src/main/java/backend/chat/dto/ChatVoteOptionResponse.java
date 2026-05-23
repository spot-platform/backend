package backend.chat.dto;

import java.util.List;

import backend.chat.entity.ChatVoteOption;
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
@Schema(description = "투표 선택지 응답 DTO")
public class ChatVoteOptionResponse {

	@Schema(description = "선택지 ID", example = "1")
	private Long id;

	@Schema(description = "선택지 내용", example = "여의도")
	private String content;

	@Schema(description = "득표 수", example = "3")
	private int voteCount;

	/**
	 * 투표한 유저 ID 목록. 익명 투표면 빈 리스트.
	 */
	@Schema(description = "투표한 유저 ID 목록 (익명 투표 시 빈 리스트)")
	private List<String> voterIds;

	public static ChatVoteOptionResponse of(ChatVoteOption option, List<String> voterIds, boolean anonymous) {
		return ChatVoteOptionResponse.builder()
			.id(option.getId())
			.content(option.getContent())
			.voteCount(option.getVoteCount())
			.voterIds(anonymous ? List.of() : voterIds)
			.build();
	}
}
