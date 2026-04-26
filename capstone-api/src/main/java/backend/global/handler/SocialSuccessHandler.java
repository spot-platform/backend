package backend.global.handler;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import backend.auth.dto.CustomOAuth2User;
import backend.auth.entity.RefreshEntity;
import backend.auth.repository.RefreshRepository;
import backend.global.util.JWTUtil;
import jakarta.servlet.http.Cookie;
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

		refreshRepository.save(
			RefreshEntity.builder()
				.email(email)
				.refresh(refreshToken)
				.build()
		);

		int maxAge = (int)(jwtUtil.getRefreshExpiry() / 1000);
		Cookie refreshCookie = new Cookie("refresh", refreshToken);
		refreshCookie.setHttpOnly(true);
		refreshCookie.setPath("/");
		refreshCookie.setMaxAge(maxAge);
		response.addCookie(refreshCookie);

		String next = request.getParameter("next");
		String redirectUrl = (next != null && !next.isBlank()) ? next : "/feed";
		response.sendRedirect(redirectUrl);
	}
}
