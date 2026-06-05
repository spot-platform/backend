package backend.feed.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import backend.chat.service.ChatService;
import backend.feed.dto.CreateOfferFeedRequest;
import backend.feed.dto.CreateRequestFeedRequest;
import backend.feed.dto.FeedApplicationResponse;
import backend.feed.dto.FeedApplyRequest;
import backend.feed.dto.FeedAuthorProfile;
import backend.feed.dto.FeedCreateResponse;
import backend.feed.dto.FeedDetailResponse;
import backend.feed.dto.FeedItemResponse;
import backend.feed.dto.FeedListQuery;
import backend.feed.dto.FeedListResponse;
import backend.feed.dto.FeedParticipantProfile;
import backend.feed.dto.PlanV3;
import backend.feed.dto.Preparation;
import backend.feed.dto.PriceBreakdown;
import backend.feed.dto.ResolvedPlace;
import backend.feed.entity.Bookmark;
import backend.feed.entity.FeedApplication;
import backend.feed.entity.FeedApplicationRole;
import backend.feed.entity.FeedApplicationStatus;
import backend.feed.entity.FeedItem;
import backend.feed.repository.BookmarkRepository;
import backend.feed.repository.FeedApplicationRepository;
import backend.feed.repository.FeedItemRepository;
import backend.global.dto.ApiResponseMeta;
import backend.global.enums.FeedAuthorRole;
import backend.global.enums.FeedItemStatus;
import backend.global.enums.FeedType;
import backend.global.error.exception.BusinessException;
import backend.global.error.exception.ErrorCode;
import backend.global.security.CustomUserDetails;
import backend.notification.service.NotificationService;
import backend.spot.entity.ParticipantRole;
import backend.spot.entity.ParticipantState;
import backend.spot.entity.Spot;
import backend.spot.entity.SpotParticipant;
import backend.spot.repository.SpotParticipantRepository;
import backend.spot.repository.SpotRepository;
import backend.user.entity.UserEntity;
import backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedItemService {

	private final FeedItemRepository feedItemRepository;
	private final FeedApplicationRepository feedApplicationRepository;
	private final BookmarkRepository bookmarkRepository;
	private final SpotRepository spotRepository;
	private final SpotParticipantRepository spotParticipantRepository;
	private final NotificationService notificationService;
	private final ChatService chatService;
	private final UserRepository userRepository;
	private final ObjectMapper objectMapper;

	public FeedListResponse getFeedItems(FeedListQuery query) {
		Pageable pageable = PageRequest.of(query.getPage(), query.getSize());
		Page<FeedItem> feedItemPage = feedItemRepository.findAllByQuery(query, pageable);
		String currentUserId = resolveCurrentUserId().orElse(null);

		List<Long> feedIds = feedItemPage.getContent().stream()
				.map(FeedItem::getId)
				.collect(Collectors.toList());
		Set<Long> bookmarkedIds = currentUserId == null
				? Collections.emptySet()
				: bookmarkRepository.findByUserIdAndFeedItemIdIn(currentUserId, feedIds)
						.stream().map(Bookmark::getFeedItemId).collect(Collectors.toSet());
		Map<Long, FeedApplication> myApplicationByFeedId = resolveMyApplicationsBatch(feedIds, currentUserId);

		List<FeedItemResponse> content = feedItemPage.getContent().stream()
				.map(feedItem -> FeedItemResponse.from(
						feedItem,
						resolveApplicantCount(feedItem),
						currentUserId == null ? null : bookmarkedIds.contains(feedItem.getId()),
						myApplicationByFeedId.get(feedItem.getId()),
						FeedItemResponse.buildAuthorProfile(feedItem),
						currentUserId))
				.collect(Collectors.toList());

		return FeedListResponse.builder()
				.data(content)
				.meta(ApiResponseMeta.builder()
						.page(feedItemPage.getNumber())
						.size(feedItemPage.getSize())
						.total(feedItemPage.getTotalElements())
						.hasNext(feedItemPage.hasNext())
						.build())
				.build();
	}

	public FeedDetailResponse getFeedItem(Long feedId) {
		FeedItem feedItem = feedItemRepository.findByIdAndDeletedFalse(feedId)
				.orElseThrow(() -> new IllegalArgumentException("피드를 찾을 수 없습니다. id=" + feedId));
		String currentUserId = resolveCurrentUserId().orElse(null);
		return FeedDetailResponse.from(
				feedItem,
				resolveApplicantCount(feedItem),
				currentUserId == null ? null : bookmarkRepository.existsByUserIdAndFeedItemId(currentUserId, feedItem.getId()),
				resolveMyApplication(feedId, currentUserId),
				FeedItemResponse.buildAuthorProfile(feedItem),
				deserialize(feedItem.getPlanJson(), PlanV3.class),
				deserialize(feedItem.getPriceBreakdownJson(), PriceBreakdown.class),
				deserialize(feedItem.getPreparationJson(), Preparation.class),
				deserializeList(feedItem.getVenueAnchorsJson()),
				deserialize(feedItem.getPrimaryPinJson(), ResolvedPlace.class),
				resolveConfirmedPartnerProfiles(feedItem.getId()),
				currentUserId);
	}

	@Transactional
	public void deleteFeedItem(Long feedId, String requesterId) {
		FeedItem feedItem = feedItemRepository.findByIdAndDeletedFalse(feedId)
				.orElseThrow(() -> new IllegalArgumentException("피드를 찾을 수 없습니다. id=" + feedId));

		if (!feedItem.getAuthorId().equals(requesterId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}

		feedItem.softDelete();
	}

	@Transactional
	public FeedApplicationResponse applyToFeed(Long feedId, String userId, String userNickname,
			FeedApplyRequest request) {
		FeedItem feedItem = feedItemRepository.findByIdAndDeletedFalse(feedId)
				.orElseThrow(() -> new IllegalArgumentException("피드를 찾을 수 없습니다. id=" + feedId));

		if (feedItem.getStatus() != backend.global.enums.FeedItemStatus.OPEN) {
			throw new IllegalStateException("모집 중인 피드에만 신청할 수 있습니다.");
		}

		feedApplicationRepository.findByFeedItemIdAndUserIdAndStatus(
						feedId, userId, FeedApplicationStatus.APPLIED)
				.ifPresent(existing -> {
					throw new IllegalStateException("이미 신청한 피드입니다.");
				});

		FeedApplication application = FeedApplication.builder()
				.feedItemId(feedId)
				.userId(userId)
				.userNickname(userNickname)
				.proposal(request.getProposal())
				.appliedRole(request.getRole())
				.deposit(request.getDeposit())
				.build();

		FeedApplicationResponse response = FeedApplicationResponse.from(feedApplicationRepository.save(application));
		if (!userId.equals(feedItem.getAuthorId())) {
			notificationService.sendAfterCommit(feedItem.getAuthorId(),
					userNickname + "님이 '" + feedItem.getTitle() + "'에 신청했어요");
		}
		return response;
	}

	@Transactional
	public void cancelApplication(Long feedId, String userId) {
		FeedItem feedItem = feedItemRepository.findByIdAndDeletedFalse(feedId)
				.orElseThrow(() -> new IllegalArgumentException("피드를 찾을 수 없습니다. id=" + feedId));
		FeedApplication application = feedApplicationRepository
				.findByFeedItemIdAndUserIdAndStatus(feedId, userId, FeedApplicationStatus.APPLIED)
				.orElseThrow(() -> new IllegalArgumentException("취소할 신청 내역이 없습니다."));

		application.cancel();
		// 작성자에게 신청 취소 알림 (자기 피드 신청 취소 케이스 제외)
		if (!userId.equals(feedItem.getAuthorId())) {
			notificationService.sendAfterCommit(feedItem.getAuthorId(),
					application.getUserNickname() + "님이 '" + feedItem.getTitle() + "' 신청을 취소했어요");
		}
	}

	@Transactional
	public void addBookmark(Long feedId, String userId) {
		feedItemRepository.findByIdAndDeletedFalse(feedId)
				.orElseThrow(() -> new IllegalArgumentException("피드를 찾을 수 없습니다. id=" + feedId));
		try {
			bookmarkRepository.saveAndFlush(Bookmark.builder()
					.userId(userId)
					.feedItemId(feedId)
					.build());
		} catch (DataIntegrityViolationException e) {
			// UNIQUE(user_id, feed_item_id) 동시 요청 경합은 idempotent no-op 처리
			if (!bookmarkRepository.existsByUserIdAndFeedItemId(userId, feedId)) {
				throw e;
			}
		}
	}

	@Transactional
	public void removeBookmark(Long feedId, String userId) {
		bookmarkRepository.findByUserIdAndFeedItemId(userId, feedId)
				.ifPresent(bookmarkRepository::delete);
	}

	@Transactional
	public FeedCreateResponse createOfferFeed(CreateOfferFeedRequest request, String authorId, String authorNickname) {
		Integer fundingGoal = request.getDesiredPrice() != null
				? request.getDesiredPrice()
				: request.getPointCost();

		FeedItem feedItem = FeedItem.builder()
				.authorId(authorId)
				.authorRole(FeedAuthorRole.SUPPORTER)
				.title(request.getTitle())
				.description(request.getContent())
				.location(request.getLocation())
				.authorNickname(authorNickname)
				.price(request.getPointCost())
				.type(FeedType.OFFER)
				.status(FeedItemStatus.OPEN)
				.spotName(request.getSpotName())
				.detailDescription(request.getDetailDescription())
				.supporterPhotoUrl(request.getSupporterPhotoUrl())
				.categoriesJson(serializeList(request.getCategories()))
				.photoUrlsJson(serializeList(request.getPhotoUrls()))
				.fundingGoal(fundingGoal)
				.maxParticipants(request.getMaxPartnerCount())
				.deadline(request.getDeadline())
				.lat(request.getLat())
				.lng(request.getLng())
				.build();

		FeedItem saved = feedItemRepository.save(feedItem);
		chatService.ensureGroupRoomForPost(String.valueOf(saved.getId()), saved.getTitle(), Set.of(authorId));
		return FeedCreateResponse.builder()
				.id(saved.getId())
				.type(saved.getType())
				.title(saved.getTitle())
				.redirectUrl("/feeds/" + saved.getId())
				.build();
	}

	@Transactional
	public FeedCreateResponse createRequestFeed(CreateRequestFeedRequest request, String authorId,
			String authorNickname) {
		Integer fundingGoal = (request.getPriceCapPerPerson() != null && request.getMaxPartnerCount() != null)
				? (int) Math.max(
						Math.min(
								(long) request.getPriceCapPerPerson() * request.getMaxPartnerCount(),
								Integer.MAX_VALUE),
						1L)
				: request.getPointCost();

		FeedItem feedItem = FeedItem.builder()
				.authorId(authorId)
				.authorRole(FeedAuthorRole.PARTNER)
				.title(request.getTitle())
				.description(request.getContent())
				.location(request.getLocation())
				.authorNickname(authorNickname)
				.price(request.getPointCost())
				.type(FeedType.REQUEST)
				.status(FeedItemStatus.OPEN)
				.spotName(request.getSpotName())
				.detailDescription(request.getDetailDescription())
				.serviceStylePhotoUrl(request.getServiceStylePhotoUrl())
				.categoriesJson(serializeList(request.getCategories()))
				.photoUrlsJson(serializeList(request.getPhotoUrls()))
				.fundingGoal(fundingGoal)
				.maxParticipants(request.getMaxPartnerCount())
				.deadline(request.getDeadline())
				.lat(request.getLat())
				.lng(request.getLng())
				.build();

		FeedItem saved = feedItemRepository.save(feedItem);
		chatService.ensureGroupRoomForPost(String.valueOf(saved.getId()), saved.getTitle(), Set.of(authorId));
		return FeedCreateResponse.builder()
				.id(saved.getId())
				.type(saved.getType())
				.title(saved.getTitle())
				.redirectUrl("/feeds/" + saved.getId())
				.build();
	}

	@Transactional
	public FeedApplicationResponse acceptApplication(Long feedId, String applicationId, String requesterId) {
		// 펀딩 달성 시 Spot 중복 생성 방지를 위해 비관적 락으로 조회
		FeedItem feedItem = feedItemRepository.findByIdAndDeletedFalseForUpdate(feedId)
				.orElseThrow(() -> new IllegalArgumentException("피드를 찾을 수 없습니다. id=" + feedId));

		if (!feedItem.getAuthorId().equals(requesterId)) {
			throw new IllegalStateException("게시글 작성자만 신청을 수락할 수 있습니다.");
		}

		FeedApplication application = feedApplicationRepository
				.findByIdAndFeedItemId(applicationId, feedId)
				.orElseThrow(() -> new IllegalArgumentException("신청 내역을 찾을 수 없습니다."));

		// 역할별 슬롯 카운트 — null/미지정 신청은 명시적으로 거부 (CodeRabbit 리뷰 반영)
		FeedApplicationRole role = application.getAppliedRole();
		if (role == null) {
			throw new IllegalStateException("지원 역할이 없는 신청은 수락할 수 없습니다.");
		}
		if (role == FeedApplicationRole.SUPPORTER) {
			if (!feedItem.canAcceptMoreSupporters()) {
				throw new IllegalStateException("이미 서포터가 수락된 피드입니다.");
			}
			feedItem.recordSupporterAccepted();
		} else if (role == FeedApplicationRole.PARTNER) {
			feedItem.recordPartnerAccepted();
		} else {
			throw new IllegalStateException("알 수 없는 지원 역할입니다: " + role);
		}

		// 기존 수락된 참여자 목록 (신규 합류 알림 대상 — 수락 처리 전에 수집, PR #125)
		List<String> existingParticipantIds = feedApplicationRepository
				.findAllByFeedItemIdAndStatus(feedId, FeedApplicationStatus.ACCEPTED)
				.stream()
				.map(FeedApplication::getUserId)
				.filter(uid -> !uid.equals(requesterId))
				.collect(Collectors.toList());

		application.accept();
		// 수락 즉시 채팅방 참여 — Spot 전환 전에도 작성자와 소통 가능하도록
		chatService.ensureGroupRoomForPost(String.valueOf(feedId), feedItem.getTitle(), Set.of(application.getUserId()));

		// 그룹 채팅방 초대 알림 → 수락된 신청자에게 (PR #125)
		if (!requesterId.equals(application.getUserId())) {
			notificationService.sendAfterCommit(application.getUserId(),
					"'" + feedItem.getTitle() + "' 그룹 채팅방에 참여됐어요");
		}
		// 새 참여자 합류 알림 → 기존 참여자들에게 (PR #125)
		existingParticipantIds.forEach(uid ->
				notificationService.sendAfterCommit(uid,
						application.getUserNickname() + "님이 '" + feedItem.getTitle() + "'에 합류했어요"));

		// 자동 Spot 전환: 매칭 조건(서포터·파트너 슬롯 충족 + maxParticipants 도달) 검사 (PR #121)
		Long convertedSpotId = null;
		if (feedItem.isReadyToMatch()) {
			convertedSpotId = convertFeedToSpot(feedItem);
		}

		// 모든 후속 처리 완료 후 수락 알림 전송 (self-action 제외)
		if (!requesterId.equals(application.getUserId())) {
			notificationService.sendAfterCommit(application.getUserId(),
					"'" + feedItem.getTitle() + "' 신청이 수락됐어요");
		}

		return FeedApplicationResponse.from(application, convertedSpotId);
	}

	@Transactional
	public FeedApplicationResponse rejectApplication(Long feedId, String applicationId, String requesterId) {
		FeedItem feedItem = feedItemRepository.findByIdAndDeletedFalse(feedId)
				.orElseThrow(() -> new IllegalArgumentException("피드를 찾을 수 없습니다. id=" + feedId));

		if (!feedItem.getAuthorId().equals(requesterId)) {
			throw new IllegalStateException("게시글 작성자만 신청을 거절할 수 있습니다.");
		}

		FeedApplication application = feedApplicationRepository
				.findByIdAndFeedItemId(applicationId, feedId)
				.orElseThrow(() -> new IllegalArgumentException("신청 내역을 찾을 수 없습니다."));

		application.reject();
		if (!requesterId.equals(application.getUserId())) {
			notificationService.sendAfterCommit(application.getUserId(),
					"'" + feedItem.getTitle() + "' 신청이 거절됐어요");
		}
		return FeedApplicationResponse.from(application);
	}

	public List<FeedApplicationResponse> getApplications(Long feedId, String requesterId) {
		FeedItem feedItem = feedItemRepository.findByIdAndDeletedFalse(feedId)
				.orElseThrow(() -> new IllegalArgumentException("피드를 찾을 수 없습니다. id=" + feedId));

		if (!feedItem.getAuthorId().equals(requesterId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}

		return feedApplicationRepository.findAllByFeedItemIdOrderByCreatedAtDesc(feedId)
				.stream()
				.map(FeedApplicationResponse::from)
				.collect(Collectors.toList());
	}

	@Transactional
	public void requestEarlyStart(Long feedId, String requesterId) {
		FeedItem feedItem = feedItemRepository.findByIdAndDeletedFalseForUpdate(feedId)
				.orElseThrow(() -> new IllegalArgumentException("피드를 찾을 수 없습니다. id=" + feedId));
		if (!feedItem.getAuthorId().equals(requesterId)) {
			throw new IllegalStateException("게시글 작성자만 조기 시작을 요청할 수 있습니다.");
		}
		if (!feedItem.canRequestEarlyStart()) {
			throw new IllegalStateException("조기 시작 요청 불가: 서포터 1명 + 파트너 1명 이상 수락 후 요청하거나, 이미 요청 중입니다.");
		}
		feedItem.requestEarlyStart();
		List<FeedApplication> accepted = feedApplicationRepository
				.findAllByFeedItemIdAndStatus(feedId, FeedApplicationStatus.ACCEPTED);
		accepted.forEach(app -> notificationService.sendAfterCommit(app.getUserId(),
				"'" + feedItem.getTitle() + "' 조기 시작 요청이 왔어요. 동의하면 Spot이 시작됩니다."));
	}

	@Transactional
	public void consentEarlyStart(Long feedId, String currentUserId) {
		// FOR UPDATE 락으로 동시 동의 요청을 직렬화한다 — 두 사용자가 동시에 동의 시
		// 두 번째 트랜잭션은 첫 번째 commit 후 진입하며, 이미 전환된 피드는 deleted=true 이므로
		// findByIdAndDeletedFalseForUpdate가 못 찾아 아래 예외로 막힌다 (hoTan35 리뷰 반영).
		FeedItem feedItem = feedItemRepository.findByIdAndDeletedFalseForUpdate(feedId)
				.orElseThrow(() -> new IllegalArgumentException(
						"피드를 찾을 수 없거나 이미 Spot으로 전환되었습니다. id=" + feedId));
		if (!feedItem.isEarlyStartRequested()) {
			throw new IllegalStateException("조기 시작 요청이 없는 피드입니다.");
		}
		List<FeedApplication> allAccepted = feedApplicationRepository
				.findAllByFeedItemIdAndStatus(feedId, FeedApplicationStatus.ACCEPTED);
		FeedApplication myApplication = allAccepted.stream()
				.filter(app -> currentUserId.equals(app.getUserId()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("수락된 신청 내역이 없습니다."));
		myApplication.consentEarlyStart();
		boolean allConsented = allAccepted.stream()
				.allMatch(app -> Boolean.TRUE.equals(app.getEarlyStartConsented()));
		if (allConsented) {
			convertFeedToSpot(feedItem);
		}
	}

	/**
	 * 피드를 Spot으로 전환하고 부수 작업(참여자 등록, 채팅방 링크, 알림)을 수행한다.
	 * @return 생성된 spotId — acceptApplication 응답에서 spotConverted/spotId 노출용
	 */
	private Long convertFeedToSpot(FeedItem feedItem) {
		Spot spot = spotRepository.save(Spot.fromFeedItem(feedItem));
		feedItem.convertToSpot(spot.getId()); // 피드는 소프트 딜리트 + spotId 보존 (PR #123)
		Set<String> participantIds = registerSpotParticipants(spot, feedItem);
		chatService.linkGroupRoomToSpot(
				String.valueOf(feedItem.getId()), String.valueOf(spot.getId()), spot.getTitle(), participantIds);
		participantIds.forEach(uid -> notificationService.sendAfterCommit(uid,
				"피드 '" + feedItem.getTitle() + "'이 Spot으로 전환됐어요!"));
		return spot.getId();
	}

	private Set<String> registerSpotParticipants(Spot spot, FeedItem feedItem) {
		List<SpotParticipant> participants = new ArrayList<>();
		participants.add(SpotParticipant.builder()
			.spotId(spot.getId())
			.userId(feedItem.getAuthorId())
			.role(ParticipantRole.AUTHOR)
			.state(ParticipantState.ACTIVE)
			.build());

		Set<String> participantIds = new HashSet<>();
		participantIds.add(feedItem.getAuthorId());

		List<FeedApplication> accepted = feedApplicationRepository
			.findAllByFeedItemIdAndStatus(feedItem.getId(), FeedApplicationStatus.ACCEPTED);
		for (FeedApplication app : accepted) {
			String uid = app.getUserId();
			if (uid != null && participantIds.add(uid)) {
				participants.add(SpotParticipant.builder()
					.spotId(spot.getId())
					.userId(uid)
					.role(ParticipantRole.PARTICIPANT)
					.applicationRole(app.getAppliedRole())
					.state(ParticipantState.ACTIVE)
					.build());
			}
		}
		spotParticipantRepository.saveAll(participants);
		return participantIds;
	}

	private Long resolveApplicantCount(FeedItem feedItem) {
		if (feedItem.getType() != FeedType.REQUEST) {
			return null;
		}
		return feedApplicationRepository.countByFeedItemIdAndStatus(feedItem.getId(), FeedApplicationStatus.APPLIED);
	}

	private FeedApplication resolveMyApplication(Long feedItemId, String currentUserId) {
		if (currentUserId == null) {
			return null;
		}
		return feedApplicationRepository.findAllByFeedItemIdAndUserId(feedItemId, currentUserId)
				.stream()
				.reduce((a, b) -> {
					if (a.getStatus() == FeedApplicationStatus.ACCEPTED) return a;
					if (b.getStatus() == FeedApplicationStatus.ACCEPTED) return b;
					return a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b;
				})
				.orElse(null);
	}

	private Map<Long, FeedApplication> resolveMyApplicationsBatch(List<Long> feedItemIds, String currentUserId) {
		if (currentUserId == null || feedItemIds.isEmpty()) {
			return Collections.emptyMap();
		}
		return feedApplicationRepository.findAllByFeedItemIdInAndUserId(feedItemIds, currentUserId)
				.stream()
				.collect(Collectors.toMap(
						FeedApplication::getFeedItemId,
						a -> a,
						(a, b) -> {
						// ACCEPTED 상태 우선 — 재신청으로 최신 APPLIED가 생겨도 isOwner 오판 방지
						if (a.getStatus() == FeedApplicationStatus.ACCEPTED) {
							return a;
						}
						if (b.getStatus() == FeedApplicationStatus.ACCEPTED) {
							return b;
						}
						return a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b;
					}));
	}

	private Optional<String> resolveCurrentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			return Optional.empty();
		}
		Object principal = authentication.getPrincipal();
		if (principal instanceof CustomUserDetails customUserDetails) {
			return Optional.ofNullable(customUserDetails.getUserId());
		}
		return Optional.empty();
	}

	private List<FeedParticipantProfile> resolveConfirmedPartnerProfiles(Long feedItemId) {
		List<FeedApplication> applications = feedApplicationRepository.findAllByFeedItemIdAndStatus(
				feedItemId, FeedApplicationStatus.ACCEPTED);
		if (applications.isEmpty()) {
			return Collections.emptyList();
		}
		List<String> userIds = applications.stream()
				.map(FeedApplication::getUserId)
				.distinct()
				.collect(Collectors.toList());
		Map<String, UserEntity> usersById = userRepository.findAllByIdIn(userIds).stream()
				.collect(Collectors.toMap(UserEntity::getId, Function.identity()));
		return applications.stream()
				.map(application -> {
					UserEntity user = usersById.get(application.getUserId());
					return FeedParticipantProfile.builder()
							.id(application.getUserId())
							.nickname(user == null ? application.getUserNickname() : user.getNickname())
							.avatarUrl(user == null ? null : user.getAvatarUrl())
							.build();
				})
				.collect(Collectors.toList());
	}

	private <T> T deserialize(String json, Class<T> valueType) {
		if (json == null || json.isBlank()) {
			return null;
		}
		try {
			return objectMapper.readValue(json, valueType);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("피드 컨텍스트 JSON 역직렬화에 실패했습니다.", e);
		}
	}

	private String serializeList(List<String> list) {
		if (list == null || list.isEmpty()) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(list);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("리스트 직렬화에 실패했습니다.", e);
		}
	}

	private List<ResolvedPlace> deserializeList(String json) {
		if (json == null || json.isBlank()) {
			return null;
		}
		try {
			return objectMapper.readValue(json, new TypeReference<>() {
			});
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("피드 장소 앵커 JSON 역직렬화에 실패했습니다.", e);
		}
	}
}
