package backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
	@NotBlank(message = "이메일은 필수입니다")
	@Email(message = "이메일 형식이 올바르지 않습니다")
	@Schema(description = "이메일", example = "user@example.com")
	String email,

	@NotBlank(message = "비밀번호는 필수입니다")
	@Schema(description = "비밀번호", example = "pass1234")
	String password
) {
}
