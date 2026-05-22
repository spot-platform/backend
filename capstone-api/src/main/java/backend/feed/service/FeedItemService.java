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

		List<String> feedIds = feedItemPage.getContent().stream()
				.map(FeedItem::getId)
				.collect(Collectors.toList());
		Set<String> bookmarkedIds = currentUserId == null
				? Collections.emptySet()
				: bookmarkRepository.findByUserIdAndFeedItemIdIn(currentUserId, feedIds)
						.stream().map(Bookmark::getFeedItemId).collect(Collectors.toSet());
		Map<String, FeedApplication> myApplicationByFeedId = resolveMyApplicationsBatch(feedIds, currentUserId);

		List<FeedItemResponse> content = feedItemPage.getContent().stream()
				.map(feedItem -> FeedItemResponse.from(
						feedItem,
						resolveApplicantCount(feedItem),
						currentUserId == null ? null : bookmarkedIds.contains(feedItem.getId()),
						myApplicationByFeedId.get(feedItem.getId()),
						FeedItemResponse.buildAuthorProfile(feedItem)))
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

	public FeedDetailResponse getFeedItem(String feedId) {
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
				resolveConfirmedPartnerProfiles(feedItem.getId()));
	}

	@Transactional
	public void deleteFeedItem(String feedId, String requesterId) {
		FeedItem feedItem = feedItemRepository.findByIdAndDeletedFalse(feedId)
				.orElseThrow(() -> new IllegalArgumentException("피드를 찾을 수 없습니다. id=" + feedId));

		if (!feedItem.getAuthorId().equals(requesterId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}

		feedItem.softDelete();
	}

	@Transactional
	public FeedApplicationResponse applyToFeed(String feedId, String userId, String userNickname,
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

		return FeedApplicationResponse.from(feedApplicationRepository.save(application));
	}

	@Transactional
	public void cancelApplication(String feedId, String userId) {
		FeedApplication application = feedApplicationRepository
				.findByFeedItemIdAndUserIdAndStatus(feedId, userId, FeedApplicationStatus.APPLIED)
				.orElseThrow(() -> new IllegalArgumentException("취소할 신청 내역이 없습니다."));

		application.cancel();
	}

	@Transactional
	public void addBookmark(String feedId, String userId) {
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
	public void removeBookmark(String feedId, String userId) {
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
		chatService.ensureGroupRoomForPost(saved.getId(), Set.of(authorId));
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
		chatService.ensureGroupRoomForPost(saved.getId(), Set.of(authorId));
		return FeedCreateResponse.builder()
				.id(saved.getId())
				.type(saved.getType())
				.title(saved.getTitle())
				.redirectUrl("/feeds/" + saved.getId())
				.build();
	}

	@Transactional
	public FeedApplicationResponse acceptApplication(String feedId, String applicationId, String requesterId) {
		FeedItem feedItem = feedItemRepository.findByIdAndDeletedFalse(feedId)
				.orElseThrow(() -> new IllegalArgumentException("피드를 찾을 수 없습니다. id=" + feedId));

		if (!feedItem.getAuthorId().equals(requesterId)) {
			throw new IllegalStateException("게시글 작성자만 신청을 수락할 수 있습니다.");
		}

		FeedApplication application = feedApplicationRepository
				.findByIdAndFeedItemId(applicationId, feedId)
				.orElseThrow(() -> new IllegalArgumentException("신청 내역을 찾을 수 없습니다."));

		application.accept();
		feedItem.accumulateFunding(feedItem.getPrice());

		if (feedItem.isFundingGoalMet()) {
			Spot spot = spotRepository.save(Spot.fromFeedItem(feedItem));
			feedItem.softDelete(); // 피드는 소프트 딜리트 (스팟으로 전환됨)
			Set<String> participantIds = registerSpotParticipants(spot, feedItem);
			chatService.linkGroupRoomToSpot(feedId, spot.getId(), participantIds);
			try {
				notificationService.send(
						feedItem.getAuthorId(),
						"피드 '" + feedItem.getTitle() + "'의 매칭이 완료되어 Spot이 생성되었습니다.");
			} catch (Exception e) {
				log.warn("[notification] Spot 생성 후 알림 전송 실패 - feedId={}, error={}",
						feedItem.getId(), e.getMessage());
			}
		}

		return FeedApplicationResponse.from(application);
	}

	@Transactional
	public FeedApplicationResponse rejectApplication(String feedId, String applicationId, String requesterId) {
		FeedItem feedItem = feedItemRepository.findByIdAndDeletedFalse(feedId)
				.orElseThrow(() -> new IllegalArgumentException("피드를 찾을 수 없습니다. id=" + feedId));

		if (!feedItem.getAuthorId().equals(requesterId)) {
			throw new IllegalStateException("게시글 작성자만 신청을 거절할 수 있습니다.");
		}

		FeedApplication application = feedApplicationRepository
				.findByIdAndFeedItemId(applicationId, feedId)
				.orElseThrow(() -> new IllegalArgumentException("신청 내역을 찾을 수 없습니다."));

		application.reject();
		return FeedApplicationResponse.from(application);
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

	private FeedApplication resolveMyApplication(String feedItemId, String currentUserId) {
		if (currentUserId == null) {
			return null;
		}
		return feedApplicationRepository
				.findFirstByFeedItemIdAndUserIdOrderByCreatedAtDesc(feedItemId, currentUserId)
				.orElse(null);
	}

	private Map<String, FeedApplication> resolveMyApplicationsBatch(List<String> feedItemIds, String currentUserId) {
		if (currentUserId == null || feedItemIds.isEmpty()) {
			return Collections.emptyMap();
		}
		return feedApplicationRepository.findAllByFeedItemIdInAndUserId(feedItemIds, currentUserId)
				.stream()
				.collect(Collectors.toMap(
						FeedApplication::getFeedItemId,
						a -> a,
						(a, b) -> a.getCreatedAt().isAfter(b.getCreatedAt()) ? a : b));
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

	private List<FeedParticipantProfile> resolveConfirmedPartnerProfiles(String feedItemId) {
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
