package backend.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record DeleteUserRequest(
	@Schema(description = "비밀번호 (소셜 로그인 유저는 불필요)", nullable = true)
	String password
) {
}
