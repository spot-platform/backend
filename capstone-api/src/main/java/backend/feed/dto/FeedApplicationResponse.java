package backend.feed.dto;

import java.time.LocalDateTime;

import backend.feed.entity.FeedApplication;
import backend.feed.entity.FeedApplicationRole;
import backend.feed.entity.FeedApplicationStatus;
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
@Schema(description = "피드 신청 응답 DTO")
public class FeedApplicationResponse {

	private String id;
	private Long feedId;
	private String userId;
	private String userNickname;
	private String proposal;
	private FeedApplicationStatus status;
	private FeedApplicationRole appliedRole;
	private Integer deposit;
	private boolean spotConverted;
	private Long spotId;
	private LocalDateTime createdAt;

	public static FeedApplicationResponse from(FeedApplication application) {
		return from(application, null);
	}

	public static FeedApplicationResponse from(FeedApplication application, Long spotId) {
		return FeedApplicationResponse.builder()
				.id(application.getId())
				.feedId(application.getFeedItemId())
				.userId(application.getUserId())
				.userNickname(application.getUserNickname())
				.proposal(application.getProposal())
				.status(application.getStatus())
				.appliedRole(application.getAppliedRole())
				.deposit(application.getDeposit())
				.spotConverted(spotId != null)
				.spotId(spotId)
				.createdAt(application.getCreatedAt())
				.build();
	}
}
