package backend.feed.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "피드 신청 요청")
public class FeedApplyRequest {

	@Schema(description = "신청 메시지", example = "저는 공예 경험이 5년 있습니다.")
	private String proposal;
}
