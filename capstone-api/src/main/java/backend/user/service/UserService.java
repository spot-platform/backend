package backend.user.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import backend.auth.repository.RefreshRepository;
import backend.feed.entity.FeedApplication;
import backend.feed.entity.FeedItem;
import backend.feed.repository.FeedApplicationRepository;
import backend.feed.repository.FeedItemRepository;
import backend.global.error.exception.BusinessException;
import backend.global.error.exception.ErrorCode;
import backend.global.security.CustomUserDetails;
import backend.spot.entity.Spot;
import backend.spot.entity.SpotParticipant;
import backend.spot.repository.SpotParticipantRepository;
import backend.spot.repository.SpotRepository;
import backend.user.dto.request.DeleteUserRequest;
import backend.user.dto.request.JoinRequest;
import backend.user.dto.request.PasswordChangeRequest;
import backend.user.dto.request.UpdateProfileRequest;
import backend.user.dto.response.MyApplicationItemResponse;
import backend.user.dto.response.MyParticipatingSpotResponse;
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
	private final FeedApplicationRepository feedApplicationRepository;
	private final FeedItemRepository feedItemRepository;
	private final SpotParticipantRepository spotParticipantRepository;
	private final SpotRepository spotRepository;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		UserEntity user = userRepository.findByEmail(email)
			.orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
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
		if (!request.newPassword().equals(request.confirmPassword())) {
			throw new BusinessException(ErrorCode.PASSWORD_CONFIRM_MISMATCH);
		}
		if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
			throw new BusinessException(ErrorCode.INVALID_PASSWORD);
		}

		user.changePassword(passwordEncoder.encode(request.newPassword()));
		refreshRepository.deleteByEmail(email);
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

	/**
	 * 내가 신청한 피드 목록 (최신순). APPLIED/ACCEPTED/REJECTED/CANCELLED 모두 포함.
	 */
	@Transactional(readOnly = true)
	public List<MyApplicationItemResponse> getMyApplications(String email) {
		UserEntity user = findActiveUserByEmail(email);
		List<FeedApplication> applications =
			feedApplicationRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());

		Set<String> feedItemIds = applications.stream()
			.map(FeedApplication::getFeedItemId)
			.collect(Collectors.toSet());
		Map<String, FeedItem> feedItemById = feedItemIds.isEmpty()
			? Map.of()
			: feedItemRepository.findAllById(feedItemIds).stream()
				.collect(Collectors.toMap(FeedItem::getId, Function.identity()));

		return applications.stream()
			.map(app -> {
				FeedItem feedItem = feedItemById.get(app.getFeedItemId());
				String title = feedItem != null ? feedItem.getTitle() : null;
				return MyApplicationItemResponse.of(app, title);
			})
			.toList();
	}

	/**
	 * 내가 참여 중인 스팟 목록. SpotParticipant 기반 (AUTHOR + PARTICIPANT 모두).
	 */
	@Transactional(readOnly = true)
	public List<MyParticipatingSpotResponse> getMyParticipatingSpots(String email) {
		UserEntity user = findActiveUserByEmail(email);
		List<SpotParticipant> participations =
			spotParticipantRepository.findByUserId(user.getId());

		Set<String> spotIds = participations.stream()
			.map(SpotParticipant::getSpotId)
			.collect(Collectors.toSet());
		Map<String, Spot> spotById = spotIds.isEmpty()
			? Map.of()
			: spotRepository.findAllById(spotIds).stream()
				.collect(Collectors.toMap(Spot::getId, Function.identity()));

		return participations.stream()
			.filter(p -> spotById.containsKey(p.getSpotId()))
			.map(p -> MyParticipatingSpotResponse.of(p, spotById.get(p.getSpotId())))
			.toList();
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
