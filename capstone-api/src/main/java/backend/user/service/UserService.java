package backend.user.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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
import backend.feed.dto.FeedItemResponse;
import backend.feed.entity.Bookmark;
import backend.feed.entity.FeedApplication;
import backend.feed.entity.FeedApplicationStatus;
import backend.feed.entity.FeedItem;
import backend.feed.repository.BookmarkRepository;
import backend.feed.repository.FeedApplicationRepository;
import backend.feed.repository.FeedItemRepository;
import backend.global.enums.FeedItemStatus;
import backend.global.enums.FeedType;
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
	private final BookmarkRepository bookmarkRepository;
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

		Set<Long> feedItemIds = applications.stream()
			.map(FeedApplication::getFeedItemId)
			.collect(Collectors.toSet());
		Map<Long, FeedItem> feedItemById = feedItemIds.isEmpty()
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
	 * status가 null이면 전체, 지정 시 해당 상태만 반환.
	 */
	@Transactional(readOnly = true)
	public List<MyParticipatingSpotResponse> getMyParticipatingSpots(String email, FeedItemStatus status) {
		UserEntity user = findActiveUserByEmail(email);
		List<SpotParticipant> participations =
			spotParticipantRepository.findByUserIdOrderByJoinedAtDesc(user.getId());

		Set<Long> spotIds = participations.stream()
			.map(SpotParticipant::getSpotId)
			.collect(Collectors.toSet());
		Map<Long, Spot> spotById = spotIds.isEmpty()
			? Map.of()
			: spotRepository.findAllById(spotIds).stream()
				.collect(Collectors.toMap(Spot::getId, Function.identity()));

		return participations.stream()
			.filter(p -> spotById.containsKey(p.getSpotId()))
			.filter(p -> status == null || spotById.get(p.getSpotId()).getStatus() == status)
			.map(p -> MyParticipatingSpotResponse.of(p, spotById.get(p.getSpotId())))
			.toList();
	}

	/**
	 * 내가 북마크한 피드 목록. 최신 북마크 순, softDelete된 피드 제외.
	 */
	@Transactional(readOnly = true)
	public List<FeedItemResponse> getMyFavorites(String email) {
		UserEntity user = findActiveUserByEmail(email);
		List<Bookmark> bookmarks = bookmarkRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
		if (bookmarks.isEmpty()) {
			return Collections.emptyList();
		}

		List<Long> feedItemIds = bookmarks.stream().map(Bookmark::getFeedItemId).toList();
		Map<Long, FeedItem> feedItemById = feedItemRepository.findAllById(feedItemIds).stream()
			.filter(f -> !f.isDeleted())
			.collect(Collectors.toMap(FeedItem::getId, Function.identity()));

		return bookmarks.stream()
			.filter(b -> feedItemById.containsKey(b.getFeedItemId()))
			.map(b -> {
				FeedItem feedItem = feedItemById.get(b.getFeedItemId());
				return FeedItemResponse.from(
					feedItem, null, true, null, FeedItemResponse.buildAuthorProfile(feedItem));
			})
			.toList();
	}

	/**
	 * 내가 관여한 피드 전체.
	 * — 내가 작성한 피드 (삭제되지 않은 것)
	 * — 내가 신청한 피드 (모든 상태, softDelete된 피드 포함)
	 * 중복 제거 후 FeedItemResponse 형태로 반환하여 지도/카드 렌더링에 바로 사용 가능.
	 */
	@Transactional(readOnly = true)
	public List<FeedItemResponse> getMyInvolvedFeeds(String email) {
		UserEntity user = findActiveUserByEmail(email);
		String userId = user.getId();

		// 1. 내가 신청한 피드 (모든 상태, 최신순)
		List<FeedApplication> myApplications =
			feedApplicationRepository.findAllByUserIdOrderByCreatedAtDesc(userId);

		Set<Long> appliedFeedIds = myApplications.stream()
			.map(FeedApplication::getFeedItemId)
			.collect(Collectors.toSet());

		Map<Long, FeedItem> appliedFeedsById = appliedFeedIds.isEmpty()
			? Collections.emptyMap()
			: feedItemRepository.findAllById(appliedFeedIds).stream()
				.collect(Collectors.toMap(FeedItem::getId, Function.identity()));

		// ACCEPTED 우선, 그 외엔 최신 신청
		Map<Long, FeedApplication> myAppByFeedId = myApplications.stream()
			.collect(Collectors.toMap(
				FeedApplication::getFeedItemId,
				a -> a,
				(a, b) -> {
					if (a.getStatus() == FeedApplicationStatus.ACCEPTED) return a;
					if (b.getStatus() == FeedApplicationStatus.ACCEPTED) return b;
					return a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b;
				}));

		// 2. 내가 작성한 피드 (삭제되지 않은 것, 최신순)
		List<FeedItem> authoredFeeds =
			feedItemRepository.findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(userId);
		Set<Long> authoredIds = authoredFeeds.stream()
			.map(FeedItem::getId)
			.collect(Collectors.toSet());

		// 3. 합치기 — 작성한 피드 먼저, 그 다음 신청한 피드 (중복 제거)
		List<FeedItemResponse> result = new ArrayList<>();

		for (FeedItem feedItem : authoredFeeds) {
			result.add(toFeedItemResponse(feedItem, myAppByFeedId.get(feedItem.getId())));
		}

		Set<Long> seen = new HashSet<>(authoredIds);
		for (FeedApplication app : myApplications) {
			Long feedId = app.getFeedItemId();
			if (!seen.add(feedId)) {
				continue;
			}
			FeedItem feedItem = appliedFeedsById.get(feedId);
			if (feedItem != null) {
				result.add(toFeedItemResponse(feedItem, myAppByFeedId.get(feedId)));
			}
		}

		return result;
	}

	private FeedItemResponse toFeedItemResponse(FeedItem feedItem, FeedApplication myApplication) {
		Long applicantCount = feedItem.getType() == FeedType.REQUEST
			? feedApplicationRepository.countByFeedItemIdAndStatus(
				feedItem.getId(), FeedApplicationStatus.APPLIED)
			: null;
		return FeedItemResponse.from(
			feedItem,
			applicantCount,
			null, // 북마크 여부는 이 API에서 제공하지 않음
			myApplication,
			FeedItemResponse.buildAuthorProfile(feedItem));
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
