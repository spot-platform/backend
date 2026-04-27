package backend.global.handler;

import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import backend.auth.repository.RefreshRepository;
import backend.global.util.JWTUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenLogoutHandler implements LogoutHandler {

	private final RefreshRepository refreshRepository;
	private final JWTUtil jwtUtil;

	@Override
	@Transactional
	public void logout(
		HttpServletRequest request,
		HttpServletResponse response,
		Authentication authentication
	) {
		String authorization = request.getHeader("Authorization");
		if (authorization != null && authorization.startsWith("Bearer ")) {
			String accessToken = authorization.substring(7);
			if (jwtUtil.isValid(accessToken)) {
				String email = jwtUtil.getEmail(accessToken);
				refreshRepository.deleteByEmail(email);
				log.info("Refresh tokens deleted for: {}", email);
			}
		}
	}
}
