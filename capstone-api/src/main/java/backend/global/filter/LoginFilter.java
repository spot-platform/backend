package backend.global.filter;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import com.fasterxml.jackson.databind.ObjectMapper;

import backend.auth.dto.LoginResultDTO;
import backend.auth.entity.RefreshEntity;
import backend.auth.repository.RefreshRepository;
import backend.global.common.response.ApiResponse;
import backend.global.security.CustomUserDetails;
import backend.global.util.JWTUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoginFilter extends UsernamePasswordAuthenticationFilter {

	private static final String LOGIN_PATH = "/api/auth/login";

	private final JWTUtil jwtUtil;
	private final RefreshRepository refreshRepository;
	private final ObjectMapper objectMapper;

	public LoginFilter(
		AuthenticationManager authenticationManager,
		JWTUtil jwtUtil,
		RefreshRepository refreshRepository,
		ObjectMapper objectMapper
	) {
		super.setAuthenticationManager(authenticationManager);
		this.jwtUtil = jwtUtil;
		this.refreshRepository = refreshRepository;
		this.objectMapper = objectMapper;
		setRequiresAuthenticationRequestMatcher(
			new AntPathRequestMatcher(LOGIN_PATH, "POST")
		);
	}

	@Override
	public Authentication attemptAuthentication(
		HttpServletRequest request,
		HttpServletResponse response
	) throws AuthenticationException {
		try {
			LoginRequest loginRequest = objectMapper
				.readValue(request.getInputStream(), LoginRequest.class);

			UsernamePasswordAuthenticationToken authToken =
				new UsernamePasswordAuthenticationToken(
					loginRequest.email(),
					loginRequest.password()
				);

			return getAuthenticationManager().authenticate(authToken);
		} catch (IOException e) {
			log.error("Login request parsing failed", e);
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void successfulAuthentication(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain chain,
		Authentication authResult
	) throws IOException {
		CustomUserDetails userDetails = (CustomUserDetails) authResult.getPrincipal();
		String email = userDetails.getUsername();
		String role = userDetails.getRole();
		String userId = userDetails.getUserId();

		String next = request.getParameter("next");
		String redirectTo = (next != null && !next.isBlank()) ? next : "/feed";

		String accessToken = jwtUtil.createAccessToken(email, role);
		String refreshToken = jwtUtil.createRefreshToken(email, role);

		refreshRepository.save(
			RefreshEntity.builder()
				.email(email)
				.refresh(refreshToken)
				.build()
		);

		// 리프레시 토큰은 HttpOnly 쿠키로 전달 (SocialSuccessHandler와 동일한 방식)
		int maxAge = (int)(jwtUtil.getRefreshExpiry() / 1000);
		Cookie refreshCookie = new Cookie("refresh", refreshToken);
		refreshCookie.setHttpOnly(true);
		refreshCookie.setPath("/");
		refreshCookie.setMaxAge(maxAge);
		response.addCookie(refreshCookie);

		LoginResultDTO result = LoginResultDTO.builder()
			.accessToken(accessToken)
			.userId(userId)
			.redirectTo(redirectTo)
			.build();

		response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
		response.setStatus(HttpServletResponse.SC_OK);
		response.getWriter().write(objectMapper.writeValueAsString(result));
	}

	@Override
	protected void unsuccessfulAuthentication(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException failed
	) throws IOException {
		log.warn("Login failed: {}", failed.getMessage());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		ApiResponse<Void> body = ApiResponse.error(
			HttpServletResponse.SC_UNAUTHORIZED,
			"이메일 또는 비밀번호가 올바르지 않습니다."
		);
		response.getWriter().write(objectMapper.writeValueAsString(body));
	}

	private record LoginRequest(String email, String password) {
	}
}
