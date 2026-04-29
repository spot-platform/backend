package backend.global.handler;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import backend.auth.dto.CustomOAuth2User;
import backend.auth.entity.RefreshEntity;
import backend.auth.repository.RefreshRepository;
import backend.global.util.JWTUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocialSuccessHandler implements AuthenticationSuccessHandler {

	private final JWTUtil jwtUtil;
	private final RefreshRepository refreshRepository;

	@Value("${frontend.base-url}")
	private String frontendBaseUrl;

	@Override
	public void onAuthenticationSuccess(
		HttpServletRequest request,
		HttpServletResponse response,
		Authentication authentication
	) throws IOException {
		CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
		String email = oAuth2User.getEmail();
		String role = oAuth2User.getRole();

		String refreshToken = jwtUtil.createRefreshToken(email, role);

		refreshRepository.deleteByEmail(email);
		refreshRepository.save(
			RefreshEntity.builder()
				.email(email)
				.refresh(refreshToken)
				.build()
		);

		int maxAge = (int)(jwtUtil.getRefreshExpiry() / 1000);
		ResponseCookie refreshCookie = ResponseCookie.from("refresh", refreshToken)
			.httpOnly(true)
			.secure(true)
			.path("/")
			.maxAge(maxAge)
			.sameSite("Strict")
			.build();
		response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

		String redirectUrl;
		if (oAuth2User.isNewUser()) {
			redirectUrl = frontendBaseUrl + "/profile/setup";
		} else {
			String next = request.getParameter("next");
			boolean isSafeRelativePath = next != null && !next.isBlank()
				&& next.startsWith("/") && !next.startsWith("//");
			redirectUrl = isSafeRelativePath ? (frontendBaseUrl + next) : (frontendBaseUrl + "/feed");
		}
		response.sendRedirect(redirectUrl);
	}
}
