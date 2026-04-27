package backend.auth.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import backend.auth.dto.JWTResponseDTO;
import backend.auth.service.AuthService;
import backend.global.common.response.ApiResponse;
import backend.global.error.exception.BusinessException;
import backend.global.error.exception.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

@Tag(name = "Auth", description = "인증 관련 API")
@RestController
@RequestMapping("/api/jwt")
@RequiredArgsConstructor
public class JwtController {

	private final AuthService authService;

	@Operation(
		summary = "소셜 로그인 토큰 교환",
		description = "소셜 로그인 후 쿠키의 Refresh Token을 헤더 방식 Access/Refresh Token으로 교환합니다."
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200", description = "교환 성공"
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "401", description = "유효하지 않은 쿠키 토큰"
		)
	})
	@PostMapping("/exchange")
	public ApiResponse<JWTResponseDTO> exchangeSocialToken(HttpServletRequest request) {
		String refreshToken = extractRefreshFromCookie(request);
		JWTResponseDTO result = authService.exchangeSocialToken(refreshToken);
		return ApiResponse.success(result);
	}

	private String extractRefreshFromCookie(HttpServletRequest request) {
		if (request.getCookies() == null) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
		for (Cookie cookie : request.getCookies()) {
			if ("refresh".equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
	}
}
