package backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;

import backend.auth.repository.RefreshRepository;
import backend.global.filter.LoginFilter;
import backend.global.util.JWTUtil;

@Configuration
public class AuthFilterConfig {

	private final AuthenticationConfiguration authenticationConfiguration;
	private final JWTUtil jwtUtil;
	private final RefreshRepository refreshRepository;
	private final ObjectMapper objectMapper;

	public AuthFilterConfig(
		@Lazy AuthenticationConfiguration authenticationConfiguration,
		JWTUtil jwtUtil,
		RefreshRepository refreshRepository,
		ObjectMapper objectMapper
	) {
		this.authenticationConfiguration = authenticationConfiguration;
		this.jwtUtil = jwtUtil;
		this.refreshRepository = refreshRepository;
		this.objectMapper = objectMapper;
	}

	@Bean
	public LoginFilter loginFilter() throws Exception {
		AuthenticationManager authenticationManager =
			authenticationConfiguration.getAuthenticationManager();
		return new LoginFilter(authenticationManager, jwtUtil, refreshRepository, objectMapper);
	}
}
