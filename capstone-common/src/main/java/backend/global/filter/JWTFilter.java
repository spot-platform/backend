package backend.global.filter;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import backend.global.common.response.ApiResponse;
import backend.global.error.exception.ErrorCode;
import backend.global.util.JWTUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 인증 헤더의 access token 을 검증하고 {@link SecurityContextHolder} 에 인증 정보를 셋팅한다.
 *
 * <p>principal 로 {@link UserDetails} 구현체 ({@link backend.global.security.CustomUserDetails})
 * 를 사용하므로, 컨트롤러는 {@code @AuthenticationPrincipal CustomUserDetails} 를 그대로 받아
 * userId / nickname 등을 즉시 사용할 수 있다. 토큰 자체에는 sub 로 email 만 들어 있고, email →
 * UserDetails 매핑은 Spring Security 표준 인터페이스인 {@link UserDetailsService} 에 위임한다.
 * (capstone-api 의 UserService 가 구현)
 *
 * <p>Authorization 헤더가 없으면 그대로 다음 필터로 진행 — 비인증 경로 (회원가입, swagger 등)
 * 가 자연스럽게 동작하고, SecurityFilterChain 의 `permitAll` 매핑이 책임을 진다.
 */
@Slf4j
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

	private static final String BEARER_PREFIX = "Bearer ";

	private final JWTUtil jwtUtil;
	private final UserDetailsService userDetailsService;
	private final ObjectMapper objectMapper;

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String token = extractBearerToken(request);
		if (token == null) {
			filterChain.doFilter(request, response);
			return;
		}

		Claims claims;
		try {
			claims = parseAndValidate(token);
		} catch (AuthenticationRejected rejected) {
			writeError(response, rejected.errorCode);
			return;
		}

		UserDetails userDetails;
		try {
			userDetails = userDetailsService.loadUserByUsername(claims.getSubject());
		} catch (UsernameNotFoundException notFound) {
			writeError(response, ErrorCode.INVALID_TOKEN);
			return;
		}

		SecurityContextHolder.getContext().setAuthentication(
			new UsernamePasswordAuthenticationToken(
				userDetails,
				null,
				userDetails.getAuthorities()
			)
		);

		filterChain.doFilter(request, response);
	}

	private String extractBearerToken(HttpServletRequest request) {
		String authorization = request.getHeader("Authorization");
		if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
			return null;
		}
		return authorization.substring(BEARER_PREFIX.length());
	}

	private Claims parseAndValidate(String token) {
		if (!jwtUtil.isValid(token)) {
			throw new AuthenticationRejected(ErrorCode.INVALID_TOKEN);
		}
		if (jwtUtil.isExpired(token)) {
			throw new AuthenticationRejected(ErrorCode.EXPIRED_TOKEN);
		}
		Claims claims = jwtUtil.parseClaims(token);
		if (!"access".equals(claims.get("type", String.class))) {
			throw new AuthenticationRejected(ErrorCode.INVALID_TOKEN);
		}
		return claims;
	}

	private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		response.setStatus(errorCode.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
		ApiResponse<Void> body = ApiResponse.error(errorCode.getStatus().value(), errorCode.getMessage());
		response.getWriter().write(objectMapper.writeValueAsString(body));
	}

	/** doFilterInternal 내부 분기를 평탄화하기 위한 짧은 unchecked 시그널. */
	private static final class AuthenticationRejected extends RuntimeException {
		private final ErrorCode errorCode;

		AuthenticationRejected(ErrorCode errorCode) {
			super(errorCode.getMessage());
			this.errorCode = errorCode;
		}
	}
}
