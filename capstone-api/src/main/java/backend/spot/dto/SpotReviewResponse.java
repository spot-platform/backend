package backend.spot.dto;

import java.time.LocalDateTime;

import backend.spot.entity.SpotReview;
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
@Schema(description = "스팟 리뷰 응답 DTO")
public class SpotReviewResponse {

	@Schema(description = "리뷰 ID", example = "1")
	private Long id;

	@Schema(description = "스팟 ID", example = "1")
	private Long spotId;

	@Schema(description = "작성자 닉네임", example = "홍길동")
	private String reviewerNickname;

	@Schema(description = "후기 대상 닉네임", example = "한강러버")
	private String targetNickname;

	@Schema(description = "별점 (1~5)", example = "5")
	private int rating;

	@Schema(description = "후기 내용", nullable = true)
	private String comment;

	@Schema(description = "작성 일시", example = "2024-04-10T12:00:00")
	private LocalDateTime createdAt;

	public static SpotReviewResponse of(SpotReview review, String reviewerNickname) {
		return SpotReviewResponse.builder()
			.id(review.getId())
			.spotId(review.getSpotId())
			.reviewerNickname(reviewerNickname)
			.targetNickname(review.getTargetNickname())
			.rating(review.getRating())
			.comment(review.getComment())
			.createdAt(review.getCreatedAt())
			.build();
	}
}
