package backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record JWTResponseDTO(
	@Schema(description = "Access Token", example = "eyJhbGci...")
	String accessToken,

	@Schema(description = "Refresh Token", example = "eyJhbGci...")
	String refreshToken
) {
}
