package backend.spot.dto;

import java.util.List;

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
	private String label;

	@Schema(description = "득표 수", example = "3")
	private Integer voteCount;

	@Schema(description = "투표한 유저 ID 목록")
	private List<String> voterIds;

	public static SpotVoteOptionResponse of(SpotVoteOption option, List<String> voterIds) {
		return SpotVoteOptionResponse.builder()
			.id(option.getId())
			.label(option.getContent())
			.voteCount(option.getVoteCount())
			.voterIds(voterIds == null ? List.of() : List.copyOf(voterIds))
			.build();
	}
}
