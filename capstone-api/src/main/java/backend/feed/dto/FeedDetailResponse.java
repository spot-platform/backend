package backend.feed.dto;

import java.util.List;

import backend.feed.entity.FeedApplicationStatus;
import backend.feed.entity.FeedItem;
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
		return FeedDetailResponse.builder()
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
				.plan(plan)
				.priceBreakdown(priceBreakdown)
				.preparation(preparation)
				.venueAnchors(venueAnchors)
				.primaryPin(primaryPin)
				.confirmedPartnerProfiles(confirmedPartnerProfiles)
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
