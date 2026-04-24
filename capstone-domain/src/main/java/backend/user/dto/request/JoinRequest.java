package backend.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinRequest(
	@NotBlank(message = "이메일은 필수입니다")
	@Email(message = "이메일 형식이 올바르지 않습니다")
	@Schema(description = "이메일 (로그인 ID)", example = "user@example.com")
	String email,

	@NotBlank(message = "비밀번호는 필수입니다")
	@Size(min = 4, message = "비밀번호는 최소 4자 이상이어야 합니다")
	@Schema(description = "비밀번호 (최소 4자)", example = "pass1234")
	String password,

	@NotBlank(message = "닉네임은 필수입니다")
	@Schema(description = "닉네임", example = "건강한삶")
	String nickname
) {
}
