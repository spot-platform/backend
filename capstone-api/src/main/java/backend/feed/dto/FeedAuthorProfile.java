package backend.feed.dto;

import backend.global.enums.FeedAuthorRole;
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
@Schema(description = "피드 작성자 프로필 DTO")
public class FeedAuthorProfile {

	@Schema(description = "작성자 ID", example = "user-uuid")
	private String id;

	@Schema(description = "작성자 닉네임", example = "유저1")
	private String nickname;

	@Schema(description = "작성자 아바타 URL")
	private String avatarUrl;

	@Schema(description = "작성자 역할", example = "SUPPORTER")
	private FeedAuthorRole role;

	@Schema(description = "작성자 평점(SUPPORTER 전용)", example = "4.8")
	private Double rating;

	@Schema(description = "작성자 분야(SUPPORTER 전용)", example = "음식·요리")
	private String field;
}
