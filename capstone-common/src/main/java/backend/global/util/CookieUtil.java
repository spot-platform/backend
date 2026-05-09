package backend.global.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import backend.global.error.exception.BusinessException;
import backend.global.error.exception.ErrorCode;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class CookieUtil {

	private static final String REFRESH_COOKIE_NAME = "refresh";

	private CookieUtil() {}

	public static String extractRefresh(HttpServletRequest request) {
		if (request.getCookies() == null) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
		for (Cookie cookie : request.getCookies()) {
			if (REFRESH_COOKIE_NAME.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
	}

	public static void addRefreshCookie(HttpServletResponse response, String token, int maxAge) {
		ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, token)
			.httpOnly(true)
			.secure(true)
			.path("/")
			.maxAge(maxAge)
			.sameSite("Strict")
			.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	public static void clearRefreshCookie(HttpServletResponse response) {
		ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
			.httpOnly(true)
			.secure(true)
			.path("/")
			.maxAge(0)
			.sameSite("Strict")
			.build();
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}
}
