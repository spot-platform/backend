package backend.feed.dto;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import backend.feed.entity.FeedApplication;
import backend.feed.entity.FeedApplicationRole;
import backend.feed.entity.FeedApplicationStatus;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Schema(description = "피드 신청 응답 DTO")
public class FeedApplicationResponse {

	private String id;
	private String feedId;
	private String userId;
	private String proposal;
	private FeedApplicationStatus status;
	private FeedApplicationRole appliedRole;
	private Integer deposit;
	private LocalDateTime createdAt;

	public static FeedApplicationResponse from(FeedApplication application) {
		return FeedApplicationResponse.builder()
				.id(application.getId())
				.feedId(application.getFeedItemId())
				.userId(application.getUserId())
				.proposal(application.getProposal())
				.status(application.getStatus())
				.appliedRole(application.getAppliedRole())
				.deposit(application.getDeposit())
				.createdAt(application.getCreatedAt())
				.build();
	}
}
