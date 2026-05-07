package backend.feed.dto;

import java.util.List;

import backend.feed.entity.FeedApplicationStatus;
import backend.feed.entity.FeedItem;
import backend.global.enums.FeedCategory;
import backend.global.enums.FeedItemStatus;
import backend.global.enums.PostType;
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
@Schema(description = "FeedItem 상세 응답 DTO")
public class FeedDetailResponse extends FeedItemResponse {

	@Schema(description = "피드 아이템 ID", example = "uuid-string")
	private String id;

	@Schema(description = "제목", example = "피드 제목")
	private String title;

	@Schema(description = "설명")
	private String description;

	@Schema(description = "카드 이미지 URL")
	private String imageUrl;

	@Schema(description = "위치", example = "장소 위치")
	private String location;

	@Schema(description = "작성자 닉네임", example = "유저1")
	private String authorNickname;

	@Schema(description = "가격/비용", example = "5000")
	private Integer price;

	@Schema(description = "피드 타입", example = "OFFER")
	private PostType type;

	@Schema(description = "상태", example = "OPEN")
	private FeedItemStatus status;

	@Schema(description = "카테고리")
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

	@Schema(description = "북마크 여부")
	private Boolean isBookmarked;

	@Schema(description = "내 신청 상태")
	private FeedApplicationStatus myApplicationStatus;

	@Schema(description = "작성자 프로필")
	private FeedAuthorProfile authorProfile;

	@Schema(description = "전환된 Spot ID")
	private String spotId;

	@Schema(description = "AI 합성 피드 여부", example = "false")
	private boolean isAi;

	@Schema(description = "조회수", example = "0")
	private Integer views;

	@Schema(description = "좋아요수", example = "0")
	private Integer likes;

	@Schema(description = "계획")
	private PlanV3 plan;

	@Schema(description = "가격 상세")
	private PriceBreakdown priceBreakdown;

	@Schema(description = "준비물")
	private Preparation preparation;

	@Schema(description = "장소 앵커 목록")
	private List<ResolvedPlace> venueAnchors;

	@Schema(description = "주요 핀")
	private ResolvedPlace primaryPin;

	@Schema(description = "확정 파트너 프로필 목록")
	private List<FeedParticipantProfile> confirmedPartnerProfiles;

	public static FeedDetailResponse from(FeedItem feedItem, Long applicantCount, Boolean isBookmarked,
			FeedApplicationStatus myApplicationStatus, FeedAuthorProfile authorProfile, PlanV3 plan,
			PriceBreakdown priceBreakdown, Preparation preparation, List<ResolvedPlace> venueAnchors,
			ResolvedPlace primaryPin, List<FeedParticipantProfile> confirmedPartnerProfiles) {
		FeedItemResponse itemResponse = FeedItemResponse.from(
				feedItem, applicantCount, isBookmarked, myApplicationStatus, authorProfile);
		return FeedDetailResponse.builder()
				.id(itemResponse.getId())
				.title(itemResponse.getTitle())
				.description(itemResponse.getDescription())
				.imageUrl(itemResponse.getImageUrl())
				.location(itemResponse.getLocation())
				.authorNickname(itemResponse.getAuthorNickname())
				.price(itemResponse.getPrice())
				.type(itemResponse.getType())
				.status(itemResponse.getStatus())
				.category(itemResponse.getCategory())
				.maxParticipants(itemResponse.getMaxParticipants())
				.deadline(itemResponse.getDeadline())
				.partnerCount(itemResponse.getPartnerCount())
				.progressPercent(itemResponse.getProgressPercent())
				.applicantCount(itemResponse.getApplicantCount())
				.isBookmarked(itemResponse.getIsBookmarked())
				.myApplicationStatus(itemResponse.getMyApplicationStatus())
				.authorProfile(itemResponse.getAuthorProfile())
				.spotId(itemResponse.getSpotId())
				.isAi(itemResponse.isAi())
				.views(itemResponse.getViews())
				.likes(itemResponse.getLikes())
				.plan(plan)
				.priceBreakdown(priceBreakdown)
				.preparation(preparation)
				.venueAnchors(venueAnchors)
				.primaryPin(primaryPin)
				.confirmedPartnerProfiles(confirmedPartnerProfiles)
				.build();
	}
}
