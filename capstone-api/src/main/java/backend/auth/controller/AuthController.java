package backend.auth.controller;

import java.io.IOException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import backend.auth.dto.JWTResponseDTO;
import backend.auth.dto.LoginRequest;
import backend.auth.dto.LoginResultDTO;
import backend.auth.dto.RefreshResponseDTO;
import backend.auth.service.AuthService;
import backend.global.common.response.ApiResponse;
import backend.global.util.CookieUtil;
import backend.global.util.JWTUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Auth", description = "인증 관련 API")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final JWTUtil jwtUtil;

	/**
	 * [Swagger 문서용 stub]
	 * 실제 처리는 LoginFilter (Spring Security)가 담당.
	 * 이 메서드는 절대 실행되지 않음.
	 */
	@Operation(
		summary = "자체 로그인",
		description = "이메일/비밀번호로 로그인합니다. 성공 시 Access Token을 반환하고 Refresh Token은 HttpOnly 쿠키로 전달합니다."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200", description = "로그인 성공"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401", description = "이메일 또는 비밀번호 불일치"
		)
	})
	@PostMapping("/login")
	public ApiResponse<LoginResultDTO> login(@Valid @RequestBody LoginRequest request) {
		throw new UnsupportedOperationException("Handled by LoginFilter");
	}

	/**
	 * [Swagger 문서용 stub]
	 * 실제 처리는 Spring Security LogoutFilter가 담당.
	 * 이 메서드는 절대 실행되지 않음.
	 */
	@Operation(
		summary = "로그아웃",
		description = "Refresh Token을 DB에서 삭제하고 refresh 쿠키를 제거합니다. Authorization 헤더에 Access Token 필요."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200", description = "로그아웃 성공"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401", description = "인증 필요"
		)
	})
	@PostMapping("/logout")
	public ApiResponse<Void> logout() {
		throw new UnsupportedOperationException("Handled by LogoutFilter");
	}

	@Operation(
		summary = "소셜 로그인 시작",
		description = "OAuth2 소셜 로그인을 시작합니다. provider는 naver 또는 google."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "302", description = "소셜 로그인 페이지로 리다이렉트"
		)
	})
	@GetMapping("/oauth/{provider}/start")
	public void oauthStart(
		@PathVariable String provider,
		HttpServletResponse response
	) throws IOException {
		response.sendRedirect("/api/v1/auth/oauth/" + provider);
	}

	@Operation(
		summary = "Access 토큰 재발급",
		description = "쿠키의 refresh 토큰으로 Access Token을 재발급합니다. Refresh Rotation 적용."
	)
	@Parameter(
		name = "refresh",
		in = ParameterIn.COOKIE,
		description = "Refresh Token (HttpOnly cookie)",
		required = true,
		schema = @Schema(type = "string")
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200", description = "재발급 성공"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401", description = "유효하지 않거나 만료된 Refresh Token"
		)
	})
	@PostMapping("/refresh")
	public ApiResponse<RefreshResponseDTO> refresh(
		HttpServletRequest request,
		HttpServletResponse response
	) {
		String refreshToken = CookieUtil.extractRefresh(request);
		JWTResponseDTO result = authService.refresh(refreshToken);
		CookieUtil.addRefreshCookie(response, result.refreshToken(), (int)(jwtUtil.getRefreshExpiry() / 1000));
		return ApiResponse.success(new RefreshResponseDTO(result.accessToken()));
	}
}
