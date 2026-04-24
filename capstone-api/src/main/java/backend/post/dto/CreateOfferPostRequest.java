package backend.post.dto;

import java.util.List;

import backend.global.enums.PostSpotCategory;
import backend.global.enums.PostType;
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
@Schema(description = "Offer 게시글 생성 요청")
public class CreateOfferPostRequest {

	@Schema(description = "게시글 타입", example = "OFFER", requiredMode = Schema.RequiredMode.REQUIRED)
	private PostType type;

	@Schema(description = "스팟 명칭", example = "한강 공원", requiredMode = Schema.RequiredMode.REQUIRED)
	private String spotName;

	@Schema(description = "제목", example = "치킨과 맥주를 즐기는 스팟 제공", requiredMode = Schema.RequiredMode.REQUIRED)
	private String title;

	@Schema(description = "요약 내용", example = "여의도 한강공원 명당 자리 제공합니다.", requiredMode = Schema.RequiredMode.REQUIRED)
	private String content;

	@Schema(description = "카테고리 목록", example = "[\"음식·요리\", \"BBQ·조개\"]", requiredMode = Schema.RequiredMode.REQUIRED)
	private List<PostSpotCategory> categories;

	@Schema(description = "사진 URL 목록", requiredMode = Schema.RequiredMode.REQUIRED)
	private List<String> photoUrls;

	@Schema(description = "포인트 비용", example = "5000", requiredMode = Schema.RequiredMode.REQUIRED)
	private Integer pointCost;

	@Schema(description = "상세 설명", example = "상세한 스팟 위치와 이용 안내입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
	private String detailDescription;

	@Schema(description = "서포터 사진 URL")
	private String supporterPhotoUrl;
}
