package backend.feed.dto;

import java.time.LocalDateTime;

import backend.feed.entity.FeedApplication;
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
	private String feedId;
	private String userId;
	private String proposal;
	private FeedApplicationStatus status;
	private LocalDateTime createdAt;

	public static FeedApplicationResponse from(FeedApplication application) {
		return FeedApplicationResponse.builder()
				.id(application.getId())
				.feedId(application.getFeedItemId())
				.userId(application.getUserId())
				.proposal(application.getProposal())
				.status(application.getStatus())
				.createdAt(application.getCreatedAt())
				.build();
	}
}
