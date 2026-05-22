package backend.feed.dto;

import backend.global.enums.FeedType;
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
@Schema(description = "피드 생성 완료 응답")
public class FeedCreateResponse {

	@Schema(description = "생성된 피드 ID", example = "1")
	private Long id;

	@Schema(description = "피드 타입", example = "OFFER")
	private FeedType type;

	@Schema(description = "피드 제목", example = "한강 공원 명당 제공")
	private String title;

	@Schema(description = "생성 후 이동할 URL", example = "/feeds/1")
	private String redirectUrl;
}
