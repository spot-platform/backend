package backend.post.dto;

import java.time.LocalDateTime;

import backend.global.enums.PostType;
import backend.post.entity.Post;
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
@Schema(description = "Post 응답 DTO")
public class PostResponse {

	@Schema(description = "게시글 ID", example = "uuid-string")
	private String id;

	@Schema(description = "게시글 타입", example = "OFFER")
	private PostType type;

	@Schema(description = "장소 명칭", example = "카페 스팟")
	private String spotName;

	@Schema(description = "제목", example = "장소 제공합니다")
	private String title;

	@Schema(description = "내용", example = "상세 내용입니다")
	private String content;

	@Schema(description = "포인트 비용", example = "1000")
	private Integer pointCost;

	@Schema(description = "작성자 닉네임", example = "유저1")
	private String authorNickname;

	@Schema(description = "생성 일시", example = "2024-04-10T10:00:00")
	private LocalDateTime createdAt;

	public static PostResponse from(Post post) {
		return PostResponse.builder()
				.id(post.getId())
				.type(post.getType())
				.spotName(post.getSpotName())
				.title(post.getTitle())
				.content(post.getContent())
				.pointCost(post.getPointCost())
				.authorNickname(post.getAuthorNickname())
				.createdAt(post.getCreatedAt())
				.build();
	}
}
