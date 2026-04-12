package backend.post.dto;

import backend.global.enums.PostType;

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
@Schema(description = "게시글 등록 완료 응답")
public class PostCompletionResponse {

	@Schema(description = "생성된 게시글 ID", example = "uuid-string")
	private String id;

	@Schema(description = "게시글 타입", example = "OFFER")
	private PostType type;

	@Schema(description = "게시글 제목", example = "한강 공원 스팟 제공")
	private String title;

	@Schema(description = "등록 완료 후 이동할 URL", example = "/posts/offer/uuid-string")
	private String redirectUrl;
}
