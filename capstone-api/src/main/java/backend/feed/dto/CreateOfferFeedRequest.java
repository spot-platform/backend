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
@Schema(description = "Offer 피드 생성 요청")
public class CreateOfferFeedRequest {

	@Schema(description = "스팟 명칭", example = "한강 공원 명당 자리")
	private String spotName;

	@NotBlank
	@Schema(description = "제목", example = "치킨·맥주 즐기는 한강 명당 제공", requiredMode = Schema.RequiredMode.REQUIRED)
	private String title;

	@NotBlank
	@Schema(description = "요약 내용", example = "여의도 한강공원 그늘 자리 제공합니다.", requiredMode = Schema.RequiredMode.REQUIRED)
	private String content;

	@Schema(description = "카테고리 목록 (FeedCategory enum 값)", example = "[\"음악\", \"운동\"]")
	private List<String> categories;

	@Schema(description = "사진 URL 목록", example = "[\"https://example.com/photo1.jpg\"]")
	private List<String> photoUrls;

	@NotNull
	@Positive
	@Schema(description = "1인당 포인트 비용", example = "5000", requiredMode = Schema.RequiredMode.REQUIRED)
	private Integer pointCost;

	@NotBlank
	@Schema(description = "위치", example = "서울 영등포구 여의도동", requiredMode = Schema.RequiredMode.REQUIRED)
	private String location;

	@Schema(description = "마감일 (YYYY-MM-DD)", example = "2026-06-30")
	private String deadline;

	@Schema(description = "상세 설명", example = "돗자리·그늘막 포함, 바베큐 가능 구역입니다.")
	private String detailDescription;

	@Schema(description = "서포터 프로필 사진 URL", example = "https://example.com/supporter.jpg")
	private String supporterPhotoUrl;

	@Schema(description = "희망 총 펀딩 목표액", example = "25000")
	private Integer desiredPrice;

	@Schema(description = "최대 파트너 수", example = "5")
	private Integer maxPartnerCount;

	@Schema(description = "위도", example = "37.5266")
	private Double lat;

	@Schema(description = "경도", example = "126.9324")
	private Double lng;
}
