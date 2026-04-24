package backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

import backend.global.filter.JWTFilter;
import backend.global.util.JWTUtil;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	private final JWTUtil jwtUtil;
	private final OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService;
	private final AuthenticationSuccessHandler socialSuccessHandler;
	private final LogoutHandler refreshTokenLogoutHandler;

	public SecurityConfig(
		JWTUtil jwtUtil,
		OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService,
		AuthenticationSuccessHandler socialSuccessHandler,
		LogoutHandler refreshTokenLogoutHandler
	) {
		this.jwtUtil = jwtUtil;
		this.oAuth2UserService = oAuth2UserService;
		this.socialSuccessHandler = socialSuccessHandler;
		this.refreshTokenLogoutHandler = refreshTokenLogoutHandler;
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
	public SecurityFilterChain securityFilterChain(
		HttpSecurity http,
		UsernamePasswordAuthenticationFilter loginFilter
	) throws Exception {
		http
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.sessionManagement(session ->
				session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)
			.cors(cors -> { })
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(
					"/api/auth/**",
					"/api/users/exist",
					"/api/users",
					"/api/jwt/exchange",
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
					endpoint.baseUri("/api/auth/oauth")
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
				.logoutUrl("/api/auth/logout")
				.addLogoutHandler(refreshTokenLogoutHandler)
				.logoutSuccessHandler((request, response, authentication) ->
					response.setStatus(200)
				)
			)
			.addFilterBefore(new JWTFilter(jwtUtil), UsernamePasswordAuthenticationFilter.class)
			.addFilterAt(loginFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
