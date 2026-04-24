package backend.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
public record LoginResultDTO(
	@Schema(description = "Access Token (1시간 유효)", example = "eyJhbGci...")
	String accessToken,

	@Schema(description = "Refresh Token (7일 유효)", example = "eyJhbGci...")
	String refreshToken,

	@Schema(description = "유저 ID", example = "uuid-string")
	String userId,

	@Schema(description = "로그인 후 이동할 경로", example = "/feed")
	String redirectTo
) {
}
