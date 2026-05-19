package backend.global.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import backend.global.error.JwtAccessDeniedHandler;
import backend.global.error.JwtAuthenticationEntryPoint;
import backend.global.filter.JWTFilter;
import backend.global.util.CookieUtil;
import backend.global.util.JWTUtil;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final String frontendBaseUrl;

	private final JWTUtil jwtUtil;
	private final ObjectMapper objectMapper;
	private final OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService;
	private final AuthenticationSuccessHandler socialSuccessHandler;
	private final LogoutHandler refreshTokenLogoutHandler;
	private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final JwtAccessDeniedHandler jwtAccessDeniedHandler;

	public SecurityConfig(
		@Value("${frontend.base-url}") String frontendBaseUrl,
		JWTUtil jwtUtil,
		ObjectMapper objectMapper,
		OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService,
		AuthenticationSuccessHandler socialSuccessHandler,
		LogoutHandler refreshTokenLogoutHandler,
		JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint,
		JwtAccessDeniedHandler jwtAccessDeniedHandler
	) {
		this.frontendBaseUrl = frontendBaseUrl;
		this.jwtUtil = jwtUtil;
		this.objectMapper = objectMapper;
		this.oAuth2UserService = oAuth2UserService;
		this.socialSuccessHandler = socialSuccessHandler;
		this.refreshTokenLogoutHandler = refreshTokenLogoutHandler;
		this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
		this.jwtAccessDeniedHandler = jwtAccessDeniedHandler;
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration config = new CorsConfiguration();
		config.setAllowedOrigins(List.of(frontendBaseUrl));
		config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		config.setAllowedHeaders(List.of(
			"Authorization",
			"Content-Type",
			"Accept",
			"Origin",
			"X-Requested-With"
		));
		config.setAllowCredentials(true);
		config.setMaxAge(3600L);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return source;
	}

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(
		AuthenticationConfiguration configuration
	) throws Exception {
		return configuration.getAuthenticationManager();
	}

	@Bean
	public JWTFilter jwtFilter(
		org.springframework.security.core.userdetails.UserDetailsService userDetailsService
	) {
		return new JWTFilter(jwtUtil, userDetailsService, objectMapper);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		UsernamePasswordAuthenticationFilter loginFilter,
		JWTFilter jwtFilter
	) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.sessionManagement(session ->
				session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.POST,
					"/api/v1/users/exist",
					"/api/v1/users",
					"/api/v1/jwt/exchange"
				).permitAll()
				.requestMatchers(HttpMethod.GET, "/api/v1/sim/**").permitAll()
				.requestMatchers(
					"/api/v1/auth/**",
					"/api/v1/spots/**",   // TODO: 인증 도입 후 제거
					"/api/v1/chat/**",    // TODO: 인증 도입 후 제거
					"/api/v1/feeds/**",   // TODO: 인증 도입 후 제거
					"/api/v1/posts/**",   // TODO: 인증 도입 후 제거
					"/v3/api-docs/**",
					"/swagger-ui/**",
					"/swagger-ui.html",
					"/swagger-resources/**",
					"/webjars/**"
				).permitAll()
				.anyRequest().authenticated()
			)
			.oauth2Login(oauth2 -> oauth2
				.authorizationEndpoint(endpoint ->
					endpoint.baseUri("/api/v1/auth/oauth")
				)
				.redirectionEndpoint(endpoint ->
					endpoint.baseUri("/login/oauth2/code/*")
				)
				.userInfoEndpoint(userInfo ->
					userInfo.userService(oAuth2UserService)
				)
				.successHandler(socialSuccessHandler)
			)
			.logout(logout -> logout
				.logoutUrl("/api/v1/auth/logout")
				.addLogoutHandler(refreshTokenLogoutHandler)
				.logoutSuccessHandler((request, response, authentication) -> {
					CookieUtil.clearRefreshCookie(response);
					response.setStatus(HttpServletResponse.SC_OK);
				})
			)
			.exceptionHandling(exceptions -> exceptions
				.authenticationEntryPoint(jwtAuthenticationEntryPoint)
				.accessDeniedHandler(jwtAccessDeniedHandler)
			)
			.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
			.addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
