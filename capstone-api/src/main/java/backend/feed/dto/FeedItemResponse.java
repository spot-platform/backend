package backend.feed.dto;

import backend.feed.entity.FeedItem;
import backend.global.enums.FeedItemStatus;
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
@Schema(description = "FeedItem 응답 DTO")
public class FeedItemResponse {

	@Schema(description = "피드 아이템 ID", example = "uuid-string")
	private String id;

	@Schema(description = "제목", example = "피드 제목")
	private String title;

	@Schema(description = "위치", example = "장소 위치")
	private String location;

	@Schema(description = "작성자 닉네임", example = "유저1")
	private String authorNickname;

	@Schema(description = "가격/비용", example = "5000")
	private Integer price;

	@Schema(description = "피드 타입", example = "OFFER")
	private PostType type;

	@Schema(description = "상태", example = "OPEN")
	private FeedItemStatus status;

	@Schema(description = "조회수", example = "0")
	private Integer views;

	@Schema(description = "좋아요수", example = "0")
	private Integer likes;

	public static FeedItemResponse from(FeedItem feedItem) {
		return FeedItemResponse.builder()
				.id(feedItem.getId())
				.title(feedItem.getTitle())
				.location(feedItem.getLocation())
				.authorNickname(feedItem.getAuthorNickname())
				.price(feedItem.getPrice())
				.type(feedItem.getType())
				.status(feedItem.getStatus())
				.views(feedItem.getViews())
				.likes(feedItem.getLikes())
				.build();
	}
}
