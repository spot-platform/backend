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
@Schema(description = "Request 게시글 생성 요청")
public class CreateRequestPostRequest {

	@Schema(description = "게시글 타입", example = "REQUEST", requiredMode = Schema.RequiredMode.REQUIRED)
	private PostType type;

	@Schema(description = "스팟 명칭", example = "옥상 스팟", requiredMode = Schema.RequiredMode.REQUIRED)
	private String spotName;

	@Schema(description = "제목", example = "옥상에서 캠핑하고 싶어요", requiredMode = Schema.RequiredMode.REQUIRED)
	private String title;

	@Schema(description = "요약 내용", example = "집 옥상을 캠핑 공간으로 꾸미고 싶습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
	private String content;

	@Schema(description = "카테고리 목록", example = "[\"음식·요리\", \"BBQ·조개\"]", requiredMode = Schema.RequiredMode.REQUIRED)
	private List<PostSpotCategory> categories;

	@Schema(description = "사진 URL 목록", requiredMode = Schema.RequiredMode.REQUIRED)
	private List<String> photoUrls;

	@Schema(description = "포인트 비용", example = "3000", requiredMode = Schema.RequiredMode.REQUIRED)
	private Integer pointCost;

	@Schema(description = "상세 설명", example = "어떤 식으로 공간을 꾸미고 싶은지 상세 내용입니다.", requiredMode = Schema.RequiredMode.REQUIRED)
	private String detailDescription;

	@Schema(description = "서비스 스타일 사진 URL")
	private String serviceStylePhotoUrl;
}
