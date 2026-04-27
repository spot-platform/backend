package backend.global.filter;

import java.io.IOException;
import java.util.Collections;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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

@Slf4j
@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

	private final JWTUtil jwtUtil;
	private final ObjectMapper objectMapper;

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

		if (!jwtUtil.isValid(token)) {
			sendErrorResponse(response, ErrorCode.INVALID_TOKEN);
			return;
		}

		if (jwtUtil.isExpired(token)) {
			sendErrorResponse(response, ErrorCode.EXPIRED_TOKEN);
			return;
		}

		Claims claims = jwtUtil.parseClaims(token);

		if (!"access".equals(claims.get("type", String.class))) {
			sendErrorResponse(response, ErrorCode.INVALID_TOKEN);
			return;
		}

		String email = claims.getSubject();
		String role = claims.get("role", String.class);

		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
			email,
			null,
			Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
		);

		SecurityContextHolder.getContext().setAuthentication(authentication);

		filterChain.doFilter(request, response);
	}

	private void sendErrorResponse(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		response.setStatus(errorCode.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
		ApiResponse<Void> body = ApiResponse.error(errorCode.getStatus().value(), errorCode.getMessage());
		response.getWriter().write(objectMapper.writeValueAsString(body));
	}
}
