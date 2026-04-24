package backend.user.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import backend.auth.repository.RefreshRepository;
import backend.global.error.exception.BusinessException;
import backend.global.error.exception.ErrorCode;
import backend.global.security.CustomUserDetails;
import backend.user.dto.request.DeleteUserRequest;
import backend.user.dto.request.JoinRequest;
import backend.user.dto.request.PasswordChangeRequest;
import backend.user.dto.request.UpdateProfileRequest;
import backend.user.dto.response.UserResponseDTO;
import backend.user.entity.UserEntity;
import backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

	private final UserRepository userRepository;
	private final BCryptPasswordEncoder passwordEncoder;
	private final RefreshRepository refreshRepository;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		UserEntity user = userRepository.findByEmail(email)
			.orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
		// TODO: 이메일 인증 기능 추가 시 아래 주석 해제
		// if (!user.getIsVerified()) {
		//     throw new UsernameNotFoundException("이메일 인증이 필요합니다: " + email);
		// }
		return new CustomUserDetails(user);
	}

	@Transactional
	public void join(JoinRequest request) {
		if (userRepository.existsByEmail(request.email())) {
			throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
		}
		UserEntity user = UserEntity.builder()
			.email(request.email())
			.password(passwordEncoder.encode(request.password()))
			.nickname(request.nickname())
			.build();
		userRepository.save(user);
	}

	@Transactional(readOnly = true)
	public boolean checkEmailExists(String email) {
		return userRepository.existsByEmail(email);
	}

	@Transactional(readOnly = true)
	public UserResponseDTO getMyProfile(String email) {
		UserEntity user = findActiveUserByEmail(email);
		return toUserResponse(user);
	}

	@Transactional
	public UserResponseDTO updateProfile(String email, UpdateProfileRequest request) {
		UserEntity user = findActiveUserByEmail(email);

		if (request.email() != null && !request.email().equals(email)) {
			if (userRepository.existsByEmail(request.email())) {
				throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
			}
		}

		user.updateProfile(
			request.nickname(),
			request.email(),
			request.phone(),
			request.avatarUrl()
		);
		return toUserResponse(user);
	}

	@Transactional
	public void changePassword(String email, PasswordChangeRequest request) {
		UserEntity user = findActiveUserByEmail(email);

		if (user.getIsSocial()) {
			throw new BusinessException(ErrorCode.SOCIAL_USER_CANNOT_CHANGE_PASSWORD);
		}
		if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
			throw new BusinessException(ErrorCode.INVALID_PASSWORD);
		}
		if (!request.newPassword().equals(request.confirmPassword())) {
			throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
		}

		user.changePassword(passwordEncoder.encode(request.newPassword()));
	}

	@Transactional
	public void deleteUser(String email, DeleteUserRequest request) {
		UserEntity user = findActiveUserByEmail(email);

		if (!user.getIsSocial()) {
			if (request.password() == null || request.password().isBlank()) {
				throw new BusinessException(ErrorCode.INVALID_PASSWORD);
			}
			if (!passwordEncoder.matches(request.password(), user.getPassword())) {
				throw new BusinessException(ErrorCode.INVALID_PASSWORD);
			}
		}

		user.softDelete();
		refreshRepository.deleteByEmail(email);
	}

	private UserEntity findActiveUserByEmail(String email) {
		UserEntity user = userRepository.findByEmail(email)
			.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
		if (user.getIsDeleted()) {
			throw new BusinessException(ErrorCode.USER_ALREADY_DELETED);
		}
		return user;
	}

	private UserResponseDTO toUserResponse(UserEntity user) {
		return UserResponseDTO.builder()
			.id(user.getId())
			.nickname(user.getNickname())
			.email(user.getEmail())
			.phone(user.getPhone())
			.avatarUrl(user.getAvatarUrl())
			.pointBalance(user.getPointBalance())
			.joinedAt(user.getCreatedAt() != null
				? user.getCreatedAt().toString() : null)
			.build();
	}
}
