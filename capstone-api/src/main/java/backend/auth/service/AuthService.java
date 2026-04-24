package backend.auth.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import backend.auth.dto.JWTResponseDTO;
import backend.auth.dto.RefreshRequestDTO;
import backend.auth.entity.RefreshEntity;
import backend.auth.repository.RefreshRepository;
import backend.global.error.exception.BusinessException;
import backend.global.error.exception.ErrorCode;
import backend.global.util.JWTUtil;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final JWTUtil jwtUtil;
	private final RefreshRepository refreshRepository;

	@Transactional
	public JWTResponseDTO refresh(RefreshRequestDTO request) {
		String refreshToken = request.refreshToken();

		if (!jwtUtil.isValid(refreshToken)) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
		if (jwtUtil.isExpired(refreshToken)) {
			throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
		}
		if (!"refresh".equals(jwtUtil.getType(refreshToken))) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
		if (!refreshRepository.existsByRefresh(refreshToken)) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}

		String email = jwtUtil.getEmail(refreshToken);
		String role = jwtUtil.getRole(refreshToken);

		refreshRepository.deleteByRefresh(refreshToken);

		String newAccessToken = jwtUtil.createAccessToken(email, role);
		String newRefreshToken = jwtUtil.createRefreshToken(email, role);

		refreshRepository.save(
			RefreshEntity.builder()
				.email(email)
				.refresh(newRefreshToken)
				.build()
		);

		return JWTResponseDTO.builder()
			.accessToken(newAccessToken)
			.refreshToken(newRefreshToken)
			.build();
	}

	@Transactional
	public JWTResponseDTO exchangeSocialToken(String refreshToken) {
		if (!jwtUtil.isValid(refreshToken)) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
		if (jwtUtil.isExpired(refreshToken)) {
			throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
		}
		if (!"refresh".equals(jwtUtil.getType(refreshToken))) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
		if (!refreshRepository.existsByRefresh(refreshToken)) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}

		String email = jwtUtil.getEmail(refreshToken);
		String role = jwtUtil.getRole(refreshToken);

		String newAccessToken = jwtUtil.createAccessToken(email, role);

		return JWTResponseDTO.builder()
			.accessToken(newAccessToken)
			.refreshToken(refreshToken)
			.build();
	}
}
