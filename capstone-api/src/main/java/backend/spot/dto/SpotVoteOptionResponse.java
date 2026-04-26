package backend.spot.dto;

import backend.spot.entity.SpotVoteOption;
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
public class SpotVoteOptionResponse {

	@Schema(description = "선택지 ID", example = "1")
	private Long id;

	@Schema(description = "선택지 내용", example = "여의도 한강공원")
	private String content;

	@Schema(description = "득표 수", example = "3")
	private Integer voteCount;

	public static SpotVoteOptionResponse from(SpotVoteOption option) {
		return SpotVoteOptionResponse.builder()
			.id(option.getId())
			.content(option.getContent())
			.voteCount(option.getVoteCount())
			.build();
	}
}
