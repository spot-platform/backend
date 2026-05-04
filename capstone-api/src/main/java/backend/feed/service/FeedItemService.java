package backend.feed.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import backend.feed.dto.FeedApplicationResponse;
import backend.feed.dto.FeedApplyRequest;
import backend.feed.dto.FeedItemResponse;
import backend.feed.dto.FeedListQuery;
import backend.feed.dto.FeedListResponse;
import backend.feed.entity.FeedApplication;
import backend.feed.entity.FeedApplicationStatus;
import backend.feed.entity.FeedItem;
import backend.feed.repository.FeedApplicationRepository;
import backend.feed.repository.FeedItemRepository;
import backend.global.dto.ApiResponseMeta;
import backend.post.entity.Post;
import backend.post.repository.PostRepository;
import backend.post.service.PostService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedItemService {

	private final FeedItemRepository feedItemRepository;
	private final FeedApplicationRepository feedApplicationRepository;
	private final PostRepository postRepository;
	private final PostService postService;

	public FeedListResponse getFeedItems(FeedListQuery query) {
		Pageable pageable = PageRequest.of(query.getPage(), query.getSize());
		Page<FeedItem> feedItemPage = feedItemRepository.findAllByQuery(query, pageable);

		List<FeedItemResponse> content = feedItemPage.getContent().stream()
				.map(FeedItemResponse::from)
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

	public FeedItemResponse getFeedItem(String feedId) {
		FeedItem feedItem = feedItemRepository.findByIdAndDeletedFalse(feedId)
				.orElseThrow(() -> new IllegalArgumentException("피드를 찾을 수 없습니다. id=" + feedId));
		return FeedItemResponse.from(feedItem);
	}

	@Transactional
	public void deleteFeedItem(String feedId) {
		FeedItem feedItem = feedItemRepository.findByIdAndDeletedFalse(feedId)
				.orElseThrow(() -> new IllegalArgumentException("피드를 찾을 수 없습니다. id=" + feedId));

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
}
