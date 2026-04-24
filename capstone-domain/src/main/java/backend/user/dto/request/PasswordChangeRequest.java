package backend.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record PasswordChangeRequest(
	@NotBlank(message = "현재 비밀번호는 필수입니다")
	@Schema(description = "현재 비밀번호")
	String currentPassword,

	@NotBlank(message = "새 비밀번호는 필수입니다")
	@Schema(description = "새 비밀번호")
	String newPassword,

	@NotBlank(message = "비밀번호 확인은 필수입니다")
	@Schema(description = "새 비밀번호 확인")
	String confirmPassword
) {
}
