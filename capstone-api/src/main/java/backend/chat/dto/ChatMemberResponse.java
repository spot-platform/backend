package backend.chat.dto;

import java.time.LocalDateTime;

import backend.chat.entity.ChatRoomMember;
import backend.user.entity.UserEntity;
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
@Schema(description = "채팅방 멤버 응답 DTO")
public class ChatMemberResponse {

	@Schema(description = "유저 ID", example = "user-uuid-string")
	private String userId;

	@Schema(description = "닉네임", example = "홍길동")
	private String nickname;

	@Schema(description = "프로필 이미지 URL")
	private String avatarUrl;

	@Schema(description = "입장 일시")
	private LocalDateTime joinedAt;

	public static ChatMemberResponse from(ChatRoomMember member, UserEntity user) {
		return ChatMemberResponse.builder()
			.userId(member.getUserId())
			.nickname(user == null ? null : user.getNickname())
			.avatarUrl(user == null ? null : user.getAvatarUrl())
			.joinedAt(member.getJoinedAt())
			.build();
	}
}
