package backend.feed.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
@Schema(description = "Request 피드 생성 요청")
public class CreateRequestFeedRequest {

	@Schema(description = "스팟 명칭", example = "옥상 캠핑 스팟")
	private String spotName;

	@NotBlank
	@Schema(description = "제목", example = "옥상에서 캠핑하고 싶어요", requiredMode = Schema.RequiredMode.REQUIRED)
	private String title;

	@NotBlank
	@Schema(description = "요약 내용", example = "집 옥상을 캠핑 공간으로 꾸미고 싶습니다.", requiredMode = Schema.RequiredMode.REQUIRED)
	private String content;

	@Schema(description = "카테고리 목록 (FeedCategory enum 값)", example = "[\"공예\", \"기타\"]")
	private List<String> categories;

	@Schema(description = "사진 URL 목록", example = "[\"https://example.com/photo1.jpg\"]")
	private List<String> photoUrls;

	@NotNull
	@Positive
	@Schema(description = "1인당 포인트 비용", example = "3000", requiredMode = Schema.RequiredMode.REQUIRED)
	private Integer pointCost;

	@NotBlank
	@Schema(description = "위치", example = "서울 마포구 합정동", requiredMode = Schema.RequiredMode.REQUIRED)
	private String location;

	@Schema(description = "마감일 (YYYY-MM-DD)", example = "2026-06-30")
	private String deadline;

	@Schema(description = "상세 설명", example = "조용한 주택 옥상, 야간 이용 가능합니다.")
	private String detailDescription;

	@Schema(description = "서비스 스타일 사진 URL", example = "https://example.com/style.jpg")
	private String serviceStylePhotoUrl;

	@Schema(description = "최대 파트너 수", example = "4")
	private Integer maxPartnerCount;

	@Schema(description = "1인당 최대 금액", example = "150000")
	private Integer priceCapPerPerson;

	@Schema(description = "위도", example = "37.5496")
	private Double lat;

	@Schema(description = "경도", example = "126.9138")
	private Double lng;
}
