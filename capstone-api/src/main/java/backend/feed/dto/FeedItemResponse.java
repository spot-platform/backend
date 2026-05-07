package backend.feed.dto;

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
@Schema(description = "FeedItem 응답 DTO")
public class FeedItemResponse {

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

	public static FeedItemResponse from(FeedItem feedItem) {
		return from(feedItem, null, null, null, buildAuthorProfile(feedItem));
	}

	public static FeedItemResponse from(FeedItem feedItem, Long applicantCount, Boolean isBookmarked,
			FeedApplicationStatus myApplicationStatus, FeedAuthorProfile authorProfile) {
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
				.partnerCount(feedItem.getType() == PostType.OFFER ? feedItem.getConfirmedPartnerCount() : null)
				.progressPercent(feedItem.getType() == PostType.OFFER ? calculateProgressPercent(feedItem) : null)
				.applicantCount(feedItem.getType() == PostType.REQUEST ? applicantCount : null)
				.isBookmarked(isBookmarked)
				.myApplicationStatus(myApplicationStatus)
				.authorProfile(authorProfile)
				.spotId(feedItem.getSpotId())
				.isAi(feedItem.isAi())
				.views(feedItem.getViews())
				.likes(feedItem.getLikes())
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
}
