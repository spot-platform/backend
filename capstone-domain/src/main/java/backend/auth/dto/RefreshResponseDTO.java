package backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record RefreshResponseDTO(
	@Schema(description = "Access Token", example = "eyJhbGci...")
	String accessToken
) {
}
