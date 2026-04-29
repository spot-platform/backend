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
import backend.user.entity.UserEntity;
import backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

	private final JWTUtil jwtUtil;
	private final RefreshRepository refreshRepository;
	private final UserRepository userRepository;

	@Transactional
	public JWTResponseDTO refresh(RefreshRequestDTO request) {
		String refreshToken = request.refreshToken();
		validateRefreshTokenStructure(refreshToken);

		String email = jwtUtil.getEmail(refreshToken);
		String role = jwtUtil.getRole(refreshToken);

		// DELETE 결과가 0이면 이미 사용된 토큰(replay attack) → 원자적으로 감지
		int deleted = refreshRepository.deleteAndCountByRefresh(refreshToken);
		if (deleted == 0) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}

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

	@Transactional(readOnly = true)
	public JWTResponseDTO exchangeSocialToken(String refreshToken) {
		validateRefreshTokenStructure(refreshToken);

		if (!refreshRepository.existsByRefresh(refreshToken)) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}

		String email = jwtUtil.getEmail(refreshToken);
		String role = jwtUtil.getRole(refreshToken);

		UserEntity user = userRepository.findByEmail(email)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

		String newAccessToken = jwtUtil.createAccessToken(email, role);

		return JWTResponseDTO.builder()
			.accessToken(newAccessToken)
			.refreshToken(refreshToken)
			.userId(user.getId())
			.build();
	}

	private void validateRefreshTokenStructure(String refreshToken) {
		if (!jwtUtil.isValid(refreshToken)) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
		if (jwtUtil.isExpired(refreshToken)) {
			throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
		}
		if (!"refresh".equals(jwtUtil.getType(refreshToken))) {
			throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
		}
	}
}
