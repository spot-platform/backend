package backend.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
	@Size(min = 1, message = "avatarUrl은 빈 문자열을 허용하지 않습니다")
	@Schema(description = "프로필 이미지 URL", nullable = true)
	String avatarUrl,

	@Size(min = 1, max = 20, message = "닉네임은 1~20자여야 합니다")
	@Schema(description = "닉네임", example = "건강한삶", nullable = true)
	String nickname,

	@Size(min = 1, message = "이메일은 빈 문자열을 허용하지 않습니다")
	@Email(message = "이메일 형식이 올바르지 않습니다")
	@Schema(description = "변경할 이메일", example = "new@example.com", nullable = true)
	String email,

	@Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$", message = "전화번호 형식이 올바르지 않습니다 (예: 010-1234-5678)")
	@Schema(description = "전화번호", example = "010-1234-5678", nullable = true)
	String phone
) {
}
