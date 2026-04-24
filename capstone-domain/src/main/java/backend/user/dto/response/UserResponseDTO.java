package backend.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record UserResponseDTO(
	@Schema(description = "유저 ID", example = "uuid-string")
	String id,

	@Schema(description = "닉네임", example = "건강한삶")
	String nickname,

	@Schema(description = "이메일", example = "user@example.com")
	String email,

	@Schema(description = "전화번호", nullable = true)
	String phone,

	@Schema(description = "프로필 이미지 URL", nullable = true)
	String avatarUrl,

	@Schema(description = "포인트 잔액", example = "42000")
	Long pointBalance,

	@Schema(description = "가입일 (ISO 8601)", example = "2025-11-01T09:00:00Z")
	String joinedAt
) {
}
