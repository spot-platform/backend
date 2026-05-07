package backend.feed.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

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
@Schema(description = "피드 참여자 프로필 DTO")
public class FeedParticipantProfile {

	@Schema(description = "참여자 ID", example = "user-uuid")
	private String id;

	@Schema(description = "참여자 닉네임", example = "유저1")
	private String nickname;

	@JsonProperty("avatar_url")
	@Schema(description = "참여자 아바타 URL")
	private String avatarUrl;
}
