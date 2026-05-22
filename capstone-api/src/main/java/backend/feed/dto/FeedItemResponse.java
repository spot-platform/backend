package backend.feed.dto;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import backend.feed.entity.FeedApplication;
import backend.feed.entity.FeedApplicationRole;
import backend.feed.entity.FeedApplicationStatus;
import backend.feed.entity.FeedItem;
import backend.global.enums.FeedCategory;
import backend.global.enums.FeedItemStatus;
import backend.global.enums.FeedType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "FeedItem 응답 DTO")
public class FeedItemResponse {

	@Schema(description = "피드 아이템 ID", example = "uuid-string")
	private String id;

	@Schema(description = "제목", example = "피드 제목")
	private String title;

	@Schema(description = "설명", example = "요리 기초를 함께 배워요")
	private String description;

	@Schema(description = "카드 이미지 URL", example = "https://example.com/feed-image.jpg")
	private String imageUrl;

	@Schema(description = "위치", example = "장소 위치")
	private String location;

	@Schema(description = "작성자 닉네임", example = "유저1")
	private String authorNickname;

	@Schema(description = "가격/비용", example = "5000")
	private Integer price;

	@Schema(description = "피드 타입", example = "OFFER")
	private FeedType type;

	@Schema(description = "상태", example = "OPEN")
	private FeedItemStatus status;

	@Schema(description = "카테고리", example = "요리")
	private FeedCategory category;

	@Schema(description = "최대 참여자 수", example = "5")
	private Integer maxParticipants;

	@Schema(description = "마감일(ISO date)", example = "2026-05-31")
	private String deadline;

	@Schema(description = "확정 파트너 수(OFFER 전용)", example = "2")
	private Integer partnerCount;

	@Schema(description = "펀딩 진행률(OFFER 전용)", example = "80")
	private Integer progressPercent;

	@Schema(description = "신청자 수(REQUEST 전용)", example = "3")
	private Long applicantCount;

	@Schema(description = "북마크 여부", example = "false")
	private Boolean isBookmarked;

	@Schema(description = "내 신청 상태", example = "APPLIED")
	private FeedApplicationStatus myApplicationStatus;

	@Schema(description = "내 신청 역할 (SUPPORTER | PARTNER)", example = "SUPPORTER")
	private FeedApplicationRole myApplicationRole;

	@Schema(description = "내 신청 보증금", example = "10000")
	private Integer myApplicationDeposit;

	@Schema(description = "대여 가능 여부", example = "false")
	private Boolean isRentable;

	@Schema(description = "작성자 프로필")
	private FeedAuthorProfile authorProfile;

	@Schema(description = "위도", example = "37.2636")
	private Double lat;

	@Schema(description = "경도", example = "127.0286")
	private Double lng;

	@Schema(description = "전환된 Spot ID", example = "spot-uuid-string")
	private String spotId;

	@Schema(description = "AI 합성 피드 여부", example = "false")
	private boolean isAi;

	@Schema(description = "조회수", example = "0")
	private Integer views;

	@Schema(description = "좋아요수", example = "0")
	private Integer likes;

	@Schema(description = "스팟 명칭", example = "한강 공원 명당 자리")
	private String spotName;

	@Schema(description = "상세 설명", example = "돗자리·그늘막 포함, 바베큐 가능 구역입니다.")
	private String detailDescription;

	@Schema(description = "서포터 사진 URL (OFFER 전용)", example = "https://example.com/supporter.jpg")
	private String supporterPhotoUrl;

	@Schema(description = "서비스 스타일 사진 URL (REQUEST 전용)", example = "https://example.com/style.jpg")
	private String serviceStylePhotoUrl;

	@Schema(description = "카테고리 목록", example = "[\"음악\", \"운동\"]")
	private List<String> categories;

	@Schema(description = "사진 URL 목록", example = "[\"https://example.com/photo1.jpg\", \"https://example.com/photo2.jpg\"]")
	private List<String> photoUrls;

	public static FeedItemResponse from(FeedItem feedItem) {
		return from(feedItem, null, null, null, buildAuthorProfile(feedItem));
	}

	public static FeedItemResponse from(FeedItem feedItem, Long applicantCount, Boolean isBookmarked,
			FeedApplication myApplication, FeedAuthorProfile authorProfile) {
		return FeedItemResponse.builder()
				.id(feedItem.getId())
				.title(feedItem.getTitle())
				.description(feedItem.getDescription())
				.imageUrl(feedItem.getImageUrl())
				.location(feedItem.getLocation())
				.authorNickname(feedItem.getAuthorNickname())
				.price(feedItem.getPrice())
				.type(feedItem.getType())
				.status(feedItem.getStatus())
				.category(feedItem.getCategory())
				.maxParticipants(feedItem.getMaxParticipants())
				.deadline(feedItem.getDeadline())
				.partnerCount(feedItem.getType() == FeedType.OFFER ? feedItem.getConfirmedPartnerCount() : null)
				.progressPercent(feedItem.getType() == FeedType.OFFER ? calculateProgressPercent(feedItem) : null)
				.applicantCount(feedItem.getType() == FeedType.REQUEST ? applicantCount : null)
				.isBookmarked(isBookmarked)
				.myApplicationStatus(myApplication != null ? myApplication.getStatus() : null)
				.myApplicationRole(myApplication != null ? myApplication.getAppliedRole() : null)
				.myApplicationDeposit(myApplication != null ? myApplication.getDeposit() : null)
				.isRentable(feedItem.getType() == FeedType.RENT)
				.authorProfile(authorProfile)
				.lat(feedItem.getLat())
				.lng(feedItem.getLng())
				.spotId(feedItem.getSpotId())
				.isAi(feedItem.isAi())
				.views(feedItem.getViews())
				.likes(feedItem.getLikes())
				.spotName(feedItem.getSpotName())
				.detailDescription(feedItem.getDetailDescription())
				.supporterPhotoUrl(feedItem.getSupporterPhotoUrl())
				.serviceStylePhotoUrl(feedItem.getServiceStylePhotoUrl())
				.categories(parseJsonList(feedItem.getCategoriesJson()))
				.photoUrls(parseJsonList(feedItem.getPhotoUrlsJson()))
				.build();
	}

	public static FeedAuthorProfile buildAuthorProfile(FeedItem feedItem) {
		if (feedItem.getAuthorRole() == null) {
			return null;
		}
		return FeedAuthorProfile.builder()
				.id(feedItem.getAuthorId())
				.nickname(feedItem.getAuthorNickname())
				.avatarUrl(feedItem.getAuthorAvatarUrl())
				.role(feedItem.getAuthorRole())
				.rating(feedItem.getAuthorRating() == null ? null : feedItem.getAuthorRating().doubleValue())
				.field(feedItem.getAuthorField())
				.build();
	}

	private static Integer calculateProgressPercent(FeedItem feedItem) {
		Integer fundingGoal = feedItem.getFundingGoal();
		if (fundingGoal == null || fundingGoal <= 0) {
			return null;
		}
		Integer fundedAmount = feedItem.getFundedAmount() == null ? 0 : feedItem.getFundedAmount();
		return (int) ((long) fundedAmount * 100L / fundingGoal);
	}

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	static List<String> parseJsonList(String json) {
		if (json == null || json.isBlank()) {
			return null;
		}
		try {
			return OBJECT_MAPPER.readValue(json, new TypeReference<List<String>>() {});
		} catch (JsonProcessingException e) {
			return null;
		}
	}
}
