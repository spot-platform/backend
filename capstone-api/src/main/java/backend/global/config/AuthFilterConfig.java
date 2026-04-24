package backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import backend.auth.repository.RefreshRepository;
import backend.global.filter.LoginFilter;
import backend.global.util.JWTUtil;

@Configuration
public class AuthFilterConfig {

	private final AuthenticationConfiguration authenticationConfiguration;
	private final JWTUtil jwtUtil;
	private final RefreshRepository refreshRepository;

	public AuthFilterConfig(
		@Lazy AuthenticationConfiguration authenticationConfiguration,
		JWTUtil jwtUtil,
		RefreshRepository refreshRepository
	) {
		this.authenticationConfiguration = authenticationConfiguration;
		this.jwtUtil = jwtUtil;
		this.refreshRepository = refreshRepository;
	}

	@Bean
	public UsernamePasswordAuthenticationFilter loginFilter() throws Exception {
		AuthenticationManager authenticationManager =
			authenticationConfiguration.getAuthenticationManager();
		return new LoginFilter(authenticationManager, jwtUtil, refreshRepository);
	}
}
