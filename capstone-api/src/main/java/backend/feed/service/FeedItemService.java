package backend.feed.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

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

import backend.global.error.exception.BusinessException;
import backend.global.error.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import backend.feed.dto.FeedApplicationResponse;
import backend.feed.dto.FeedApplyRequest;
import backend.feed.dto.FeedAuthorProfile;
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
import backend.global.security.CustomUserDetails;
import backend.post.entity.Post;
import backend.post.repository.PostRepository;
import backend.post.service.PostService;
import backend.user.entity.UserEntity;
import backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedItemService {

	private final FeedItemRepository feedItemRepository;
	private final FeedApplicationRepository feedApplicationRepository;
	private final BookmarkRepository bookmarkRepository;
	private final PostRepository postRepository;
	private final PostService postService;
	private final UserRepository userRepository;
	private final ObjectMapper objectMapper;

	public FeedListResponse getFeedItems(FeedListQuery query) {
		Pageable pageable = PageRequest.of(query.getPage(), query.getSize());
		Page<FeedItem> feedItemPage = feedItemRepository.findAllByQuery(query, pageable);
		String currentUserId = resolveCurrentUserId().orElse(null);

		List<FeedItemResponse> content = feedItemPage.getContent().stream()
				.map(feedItem -> FeedItemResponse.from(
						feedItem,
						resolveApplicantCount(feedItem),
						currentUserId == null ? null : false,
						resolveMyApplicationStatus(feedItem.getId(), currentUserId),
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
				currentUserId == null ? null : false,
				resolveMyApplicationStatus(feedItem.getId(), currentUserId),
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

		if (feedItem.getPostId() != null) {
			postRepository.findByIdAndDeletedFalse(feedItem.getPostId())
					.ifPresent(Post::softDelete);
		}
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
		if (!bookmarkRepository.existsByUserIdAndFeedItemId(userId, feedId)) {
			bookmarkRepository.save(Bookmark.builder()
					.userId(userId)
					.feedItemId(feedId)
					.build());
		}
	}

	@Transactional
	public void removeBookmark(String feedId, String userId) {
		bookmarkRepository.findByUserIdAndFeedItemId(userId, feedId)
				.ifPresent(bookmarkRepository::delete);
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

		if (feedItem.isFundingGoalMet() && feedItem.getPostId() != null) {
			postService.convertToSpot(feedItem.getPostId());
			feedItem.softDelete(); // 피드는 소프트 딜리트 (스팟으로 전환됨)
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

	private Long resolveApplicantCount(FeedItem feedItem) {
		if (feedItem.getType() != backend.global.enums.PostType.REQUEST) {
			return null;
		}
		return feedApplicationRepository.countByFeedItemIdAndStatus(feedItem.getId(), FeedApplicationStatus.APPLIED);
	}

	private FeedApplicationStatus resolveMyApplicationStatus(String feedItemId, String currentUserId) {
		if (currentUserId == null) {
			return null;
		}
		return feedApplicationRepository
				.findFirstByFeedItemIdAndUserIdOrderByCreatedAtDesc(feedItemId, currentUserId)
				.map(FeedApplication::getStatus)
				.orElse(null);
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
