package backend.global.util;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
public class JWTUtil {

	@Value("${jwt.secret}")
	private String secretKey;

	@Value("${jwt.access-expiry}")
	private long accessExpiry;

	@Value("${jwt.refresh-expiry}")
	private long refreshExpiry;

	public String createAccessToken(String email, String role) {
		return Jwts.builder()
			.setSubject(email)
			.claim("role", role)
			.claim("type", "access")
			.setIssuedAt(new Date())
			.setExpiration(new Date(System.currentTimeMillis() + accessExpiry))
			.signWith(SignatureAlgorithm.HS256, secretKey)
			.compact();
	}

	public String createRefreshToken(String email, String role) {
		return Jwts.builder()
			.setSubject(email)
			.claim("role", role)
			.claim("type", "refresh")
			.setIssuedAt(new Date())
			.setExpiration(new Date(System.currentTimeMillis() + refreshExpiry))
			.signWith(SignatureAlgorithm.HS256, secretKey)
			.compact();
	}

	public String getEmail(String token) {
		return getClaims(token).getSubject();
	}

	public String getRole(String token) {
		return getClaims(token).get("role", String.class);
	}

	public String getType(String token) {
		return getClaims(token).get("type", String.class);
	}

	public boolean isExpired(String token) {
		try {
			return getClaims(token).getExpiration().before(new Date());
		} catch (Exception e) {
			return true;
		}
	}

	public Claims parseClaims(String token) {
		return getClaims(token);
	}

	public boolean isValid(String token) {
		try {
			getClaims(token);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public long getRefreshExpiry() {
		return refreshExpiry;
	}

	private Claims getClaims(String token) {
		try {
			return Jwts.parser()
				.setSigningKey(secretKey)
				.parseClaimsJws(token)
				.getBody();
		} catch (ExpiredJwtException e) {
			return e.getClaims();
		}
	}
}
