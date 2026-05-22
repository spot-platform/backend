package backend.feed.dto;

import backend.global.enums.FeedCategory;
import backend.global.enums.FeedItemStatus;
import backend.global.enums.FeedType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "피드 목록 조회 파라미터")
public class FeedListQuery {
	@Schema(description = "탭 정보 (예: HOME, EXPLORE)", example = "HOME")
	private String tab;

	@Schema(description = "피드 타입", example = "OFFER")
	private FeedType type;

	@Schema(description = "피드 상태", example = "OPEN")
	private FeedItemStatus status;

	@Schema(description = "피드 카테고리", example = "요리")
	private FeedCategory category;

	@Schema(description = "정렬 조건 (latest: 최신순, popular: 인기순)", example = "latest")
	private String sort;

	@Schema(description = "근처 위도 (제공 시 nearLng 와 함께 반경 5km 필터 적용)", example = "37.2636")
	private Double nearLat;

	@Schema(description = "근처 경도", example = "127.0286")
	private Double nearLng;

	@Schema(description = "페이지 번호 (0부터 시작)", example = "0")
	private int page = 0;

	@Schema(description = "한 페이지에 보여줄 개수", example = "10")
	private int size = 10;
}
