package backend.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;

public record UpdateProfileRequest(
	@Schema(description = "프로필 이미지 URL", nullable = true)
	String avatarUrl,

	@Schema(description = "닉네임", example = "건강한삶", nullable = true)
	String nickname,

	@Email(message = "이메일 형식이 올바르지 않습니다")
	@Schema(description = "변경할 이메일", example = "new@example.com", nullable = true)
	String email,

	@Schema(description = "전화번호", example = "010-1234-5678", nullable = true)
	String phone
) {
}
