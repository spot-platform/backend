package backend.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
	@NotBlank(message = "현재 비밀번호는 필수입니다")
	@Schema(description = "현재 비밀번호")
	String currentPassword,

	@NotBlank(message = "새 비밀번호는 필수입니다")
	@Size(min = 8, max = 64, message = "새 비밀번호는 8자 이상 64자 이하여야 합니다")
	@Schema(description = "새 비밀번호 (8~64자)")
	String newPassword,

	@NotBlank(message = "비밀번호 확인은 필수입니다")
	@Schema(description = "새 비밀번호 확인")
	String confirmPassword
) {
}
