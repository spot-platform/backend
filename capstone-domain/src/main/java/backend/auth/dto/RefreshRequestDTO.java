package backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshRequestDTO(
	@NotBlank(message = "refreshToken은 필수입니다")
	@Schema(description = "Refresh Token", example = "eyJhbGci...")
	String refreshToken
) {
}
