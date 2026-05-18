package backend.post.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import backend.feed.entity.FeedItem;
import backend.feed.repository.FeedItemRepository;
import backend.global.enums.FeedItemStatus;
import backend.global.enums.PostType;
import backend.global.error.exception.BusinessException;
import backend.global.error.exception.ErrorCode;
import backend.notification.service.NotificationService;
import backend.post.dto.CreateOfferPostRequest;
import backend.post.dto.CreateRequestPostRequest;
import backend.post.dto.PostCompletionResponse;
import backend.post.dto.PostResponse;
import backend.post.entity.Post;
import backend.post.repository.PostRepository;
import backend.spot.entity.Spot;
import backend.spot.repository.SpotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

	private final PostRepository postRepository;
	private final FeedItemRepository feedItemRepository;
	private final SpotRepository spotRepository;
	private final NotificationService notificationService;

	public PostCompletionResponse createOfferPost(CreateOfferPostRequest request) {
		Post post = Post.builder()
				.type(PostType.OFFER)
				.authorId("dummy-user-id")
				.authorNickname("황호찬")
				.spotName(request.getSpotName())
				.title(request.getTitle())
				.content(request.getContent())
				.categories(request.getCategories())
				.photoUrls(request.getPhotoUrls())
				.pointCost(request.getPointCost())
				.location(request.getLocation())
				.deadline(request.getDeadline())
				.detailDescription(request.getDetailDescription())
				.supporterPhotoUrl(request.getSupporterPhotoUrl())
				.desiredPrice(request.getDesiredPrice())
				.maxPartnerCount(request.getMaxPartnerCount())
				.build();

		Post savedPost = postRepository.save(post);

		Integer fundingGoal = request.getDesiredPrice() != null
				? request.getDesiredPrice()
				: request.getPointCost();

		FeedItem feedItem = FeedItem.builder()
				.postId(savedPost.getId())
				.authorId(savedPost.getAuthorId())
				.title(savedPost.getTitle())
				.description(savedPost.getContent())
				.location(savedPost.getLocation())
				.authorNickname(savedPost.getAuthorNickname())
				.price(savedPost.getPointCost())
				.type(PostType.OFFER)
				.status(FeedItemStatus.OPEN)
				.fundingGoal(fundingGoal)
				.maxParticipants(request.getMaxPartnerCount())
				.deadline(request.getDeadline())
				.build();

		FeedItem savedFeedItem = feedItemRepository.save(feedItem);
		savedPost.linkFeedItem(savedFeedItem.getId());

		return PostCompletionResponse.builder()
				.id(savedPost.getId())
				.type(savedPost.getType())
				.title(savedPost.getTitle())
				.redirectUrl("/feed/" + savedFeedItem.getId())
				.build();
	}

	public PostCompletionResponse createRequestPost(CreateRequestPostRequest request) {
		Post post = Post.builder()
				.type(PostType.REQUEST)
				.authorId("dummy-user-id")
				.authorNickname("황호찬")
				.spotName(request.getSpotName())
				.title(request.getTitle())
				.content(request.getContent())
				.categories(request.getCategories())
				.photoUrls(request.getPhotoUrls())
				.pointCost(request.getPointCost())
				.location(request.getLocation())
				.deadline(request.getDeadline())
				.detailDescription(request.getDetailDescription())
				.serviceStylePhotoUrl(request.getServiceStylePhotoUrl())
				.maxPartnerCount(request.getMaxPartnerCount())
				.build();

		Post savedPost = postRepository.save(post);

		Integer fundingGoal = (request.getPriceCapPerPerson() != null && request.getMaxPartnerCount() != null)
				? request.getPriceCapPerPerson() * request.getMaxPartnerCount()
				: request.getPointCost();

		FeedItem feedItem = FeedItem.builder()
				.postId(savedPost.getId())
				.authorId(savedPost.getAuthorId())
				.title(savedPost.getTitle())
				.description(savedPost.getContent())
				.location(savedPost.getLocation())
				.authorNickname(savedPost.getAuthorNickname())
				.price(savedPost.getPointCost())
				.type(PostType.REQUEST)
				.status(FeedItemStatus.OPEN)
				.fundingGoal(fundingGoal)
				.maxParticipants(request.getMaxPartnerCount())
				.deadline(request.getDeadline())
				.build();

		FeedItem savedFeedItem = feedItemRepository.save(feedItem);
		savedPost.linkFeedItem(savedFeedItem.getId());

		return PostCompletionResponse.builder()
				.id(savedPost.getId())
				.type(savedPost.getType())
				.title(savedPost.getTitle())
				.redirectUrl("/feed/" + savedFeedItem.getId())
				.build();
	}

	@Transactional(readOnly = true)
	public PostResponse getPost(String postId) {
		Post post = postRepository.findByIdAndDeletedFalse(postId)
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + postId));
		return PostResponse.from(post);
	}

	public void deletePost(String postId) {
		Post post = postRepository.findByIdAndDeletedFalse(postId)
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + postId));

		post.softDelete();

		if (post.getFeedItemId() != null) {
			feedItemRepository.findByIdAndDeletedFalse(post.getFeedItemId())
					.ifPresent(FeedItem::softDelete);
		}
	}

	// FeedItemService에서 펀딩 목표 달성 시 시스템 내부 호출용
	public void convertToSpot(String postId) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + postId));
		executeConvertToSpot(post);
	}

	// 사용자 요청에 의한 호출 — 작성자 본인만 실행 가능
	public void convertToSpot(String postId, String requesterId) {
		Post post = postRepository.findById(postId)
				.orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다. id=" + postId));

		if (!post.getAuthorId().equals(requesterId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN);
		}

		executeConvertToSpot(post);
	}

	private void executeConvertToSpot(Post post) {
		if (post.getStatus() == FeedItemStatus.MATCHED) {
			return;
		}

		post.match();
		spotRepository.save(Spot.fromPost(post, post.getTitle(), post.getContent(), post.getPointCost()));

		try {
			notificationService.send(post.getAuthorId(), "게시글 '" + post.getTitle() + "'의 매칭이 완료되어 Spot이 생성되었습니다.");
		} catch (Exception e) {
			log.warn("[notification] Spot 생성 후 알림 전송 실패 - postId={}, error={}", post.getId(), e.getMessage());
		}
	}
}
