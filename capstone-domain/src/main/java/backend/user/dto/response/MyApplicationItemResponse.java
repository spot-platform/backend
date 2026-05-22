package backend.user.dto.response;

import java.time.LocalDateTime;

import backend.feed.entity.FeedApplication;
import backend.feed.entity.FeedApplicationRole;
import backend.feed.entity.FeedApplicationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "내가 신청한 피드 항목")
public class MyApplicationItemResponse {

	@Schema(description = "신청 ID")
	private String applicationId;

	@Schema(description = "피드 아이템 ID")
	private Long feedItemId;

	@Schema(description = "피드 제목")
	private String feedTitle;

	@Schema(description = "신청 상태")
	private FeedApplicationStatus status;

	@Schema(description = "신청 역할")
	private FeedApplicationRole appliedRole;

	@Schema(description = "보증금")
	private Integer deposit;

	@Schema(description = "신청 일시")
	private LocalDateTime createdAt;

	public static MyApplicationItemResponse of(FeedApplication app, String feedTitle) {
		return MyApplicationItemResponse.builder()
			.applicationId(app.getId())
			.feedItemId(app.getFeedItemId())
			.feedTitle(feedTitle)
			.status(app.getStatus())
			.appliedRole(app.getAppliedRole())
			.deposit(app.getDeposit())
			.createdAt(app.getCreatedAt())
			.build();
	}
}
