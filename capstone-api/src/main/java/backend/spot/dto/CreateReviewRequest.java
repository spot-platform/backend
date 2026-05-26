package backend.spot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "스팟 리뷰 작성 요청 DTO")
public class CreateReviewRequest {

	@NotBlank
	@Schema(description = "후기 대상 닉네임", example = "한강러버")
	private String targetNickname;

	@Min(1)
	@Max(5)
	@Schema(description = "별점 (1~5)", example = "5")
	private int rating;

	@Schema(description = "후기 내용", nullable = true)
	private String comment;
}
