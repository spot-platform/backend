package backend.chat.dto;

import java.time.LocalDateTime;

import backend.chat.entity.ChatBlock;
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
@Schema(description = "차단 응답 DTO")
public class ChatBlockResponse {

	@Schema(description = "차단 row ID", example = "1")
	private Long id;

	@Schema(description = "차단된 유저 ID", example = "user-uuid-string")
	private String blockedId;

	@Schema(description = "차단된 유저 닉네임 (조회 가능한 경우)", example = "홍길동")
	private String blockedNickname;

	@Schema(description = "차단 일시")
	private LocalDateTime createdAt;

	public static ChatBlockResponse from(ChatBlock block, UserEntity blockedUser) {
		return ChatBlockResponse.builder()
			.id(block.getId())
			.blockedId(block.getBlockedId())
			.blockedNickname(blockedUser == null ? null : blockedUser.getNickname())
			.createdAt(block.getCreatedAt())
			.build();
	}
}
