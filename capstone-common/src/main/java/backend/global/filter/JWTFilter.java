package backend.global.filter;

import java.io.IOException;
import java.util.Collections;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import backend.global.util.JWTUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

	private final JWTUtil jwtUtil;

	@Override
	protected void doFilterInternal(
		HttpServletRequest request,
		HttpServletResponse response,
		FilterChain filterChain
	) throws ServletException, IOException {
		String authorization = request.getHeader("Authorization");

		if (authorization == null || !authorization.startsWith("Bearer ")) {
			filterChain.doFilter(request, response);
			return;
		}

		String token = authorization.substring(7);

		if (!jwtUtil.isValid(token) || jwtUtil.isExpired(token)) {
			filterChain.doFilter(request, response);
			return;
		}

		if (!"access".equals(jwtUtil.getType(token))) {
			filterChain.doFilter(request, response);
			return;
		}

		String email = jwtUtil.getEmail(token);
		String role = jwtUtil.getRole(token);

		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
			email,
			null,
			Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
		);

		SecurityContextHolder.getContext().setAuthentication(authentication);

		filterChain.doFilter(request, response);
	}
}
