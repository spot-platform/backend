package backend.feed.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import backend.feed.dto.FeedApplicationResponse;
import backend.feed.dto.FeedApplyRequest;
import backend.feed.dto.FeedItemResponse;
import backend.feed.entity.FeedApplication;
import backend.feed.entity.FeedApplicationStatus;
import backend.feed.entity.FeedItem;
import backend.feed.repository.FeedApplicationRepository;
import backend.feed.repository.FeedItemRepository;
import backend.global.enums.FeedItemStatus;
import backend.global.enums.PostType;
import backend.post.repository.PostRepository;
import backend.post.service.PostService;

@ExtendWith(MockitoExtension.class)
class FeedItemServiceTest {

	@Mock
	private FeedItemRepository feedItemRepository;

	@Mock
	private FeedApplicationRepository feedApplicationRepository;

	@Mock
	private PostRepository postRepository;

	@Mock
	private PostService postService;

	@InjectMocks
	private FeedItemService feedItemService;

	// ─────────────────────────────────────────────
	// getFeedItem
	// ─────────────────────────────────────────────

	@Test
	@DisplayName("성공: 존재하는 피드 ID로 조회하면 응답이 반환된다.")
	void getFeedItem_Success() {
		FeedItem feedItem = feedItem("feed-001", "dummy-author", FeedItemStatus.OPEN, 5000, 25000, 20000);

		given(feedItemRepository.findByIdAndDeletedFalse("feed-001")).willReturn(Optional.of(feedItem));

		FeedItemResponse response = feedItemService.getFeedItem("feed-001");

		assertNotNull(response);
		assertEquals("feed-001", response.getId());
	}

	@Test
	@DisplayName("실패: 존재하지 않는 피드 ID 조회 시 예외 발생.")
	void getFeedItem_Fail_NotFound() {
		given(feedItemRepository.findByIdAndDeletedFalse("none")).willReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> feedItemService.getFeedItem("none"));
	}

	// ─────────────────────────────────────────────
	// applyToFeed
	// ─────────────────────────────────────────────

	@Test
	@DisplayName("성공: OPEN 상태 피드에 신청하면 FeedApplication이 저장된다.")
	void applyToFeed_Success() {
		FeedItem feedItem = feedItem("feed-001", "author-id", FeedItemStatus.OPEN, 5000, 25000, 0);
		FeedApplyRequest request = new FeedApplyRequest("저는 경험이 있습니다.");

		FeedApplication saved = FeedApplication.builder()
				.id("app-001")
				.feedItemId("feed-001")
				.userId("user-001")
				.userNickname("테스터")
				.proposal(request.getProposal())
				.build();

		given(feedItemRepository.findByIdAndDeletedFalse("feed-001")).willReturn(Optional.of(feedItem));
		given(feedApplicationRepository.findByFeedItemIdAndUserIdAndStatus(
				"feed-001", "user-001", FeedApplicationStatus.APPLIED)).willReturn(Optional.empty());
		given(feedApplicationRepository.save(any(FeedApplication.class))).willReturn(saved);

		FeedApplicationResponse response = feedItemService.applyToFeed(
				"feed-001", "user-001", "테스터", request);

		assertNotNull(response);
		assertEquals(FeedApplicationStatus.APPLIED, response.getStatus());
		verify(feedApplicationRepository, times(1)).save(any(FeedApplication.class));
	}

	@Test
	@DisplayName("실패: 이미 신청한 피드에 중복 신청 시 예외 발생.")
	void applyToFeed_Fail_AlreadyApplied() {
		FeedItem feedItem = feedItem("feed-001", "author-id", FeedItemStatus.OPEN, 5000, 25000, 0);
		FeedApplication existing = FeedApplication.builder()
				.id("app-000")
				.feedItemId("feed-001")
				.userId("user-001")
				.userNickname("테스터")
				.build();

		given(feedItemRepository.findByIdAndDeletedFalse("feed-001")).willReturn(Optional.of(feedItem));
		given(feedApplicationRepository.findByFeedItemIdAndUserIdAndStatus(
				"feed-001", "user-001", FeedApplicationStatus.APPLIED)).willReturn(Optional.of(existing));

		assertThrows(IllegalStateException.class,
				() -> feedItemService.applyToFeed("feed-001", "user-001", "테스터",
						new FeedApplyRequest("신청")));
	}

	@Test
	@DisplayName("실패: OPEN이 아닌 피드에 신청 시 예외 발생.")
	void applyToFeed_Fail_NotOpen() {
		FeedItem feedItem = feedItem("feed-001", "author-id", FeedItemStatus.MATCHED, 5000, 25000, 25000);

		given(feedItemRepository.findByIdAndDeletedFalse("feed-001")).willReturn(Optional.of(feedItem));

		assertThrows(IllegalStateException.class,
				() -> feedItemService.applyToFeed("feed-001", "user-001", "테스터",
						new FeedApplyRequest("신청")));
	}

	// ─────────────────────────────────────────────
	// acceptApplication — 펀딩 미달성
	// ─────────────────────────────────────────────

	@Test
	@DisplayName("성공: 수락 후 펀딩 목표 미달성이면 Spot 전환이 발생하지 않는다.")
	void acceptApplication_Success_FundingNotMet() {
		// 목표 25000, 현재 0 → 수락 1건(5000) 후 5000 → 아직 미달성
		FeedItem feedItem = feedItem("feed-001", "author-id", FeedItemStatus.OPEN, 5000, 25000, 0);
		FeedApplication application = appliedApplication("app-001", "feed-001");

		given(feedItemRepository.findByIdAndDeletedFalse("feed-001")).willReturn(Optional.of(feedItem));
		given(feedApplicationRepository.findByIdAndFeedItemId("app-001", "feed-001"))
				.willReturn(Optional.of(application));

		FeedApplicationResponse response = feedItemService.acceptApplication(
				"feed-001", "app-001", "author-id");

		assertEquals(FeedApplicationStatus.ACCEPTED, response.getStatus());
		verify(postService, never()).convertToSpot(any());
	}

	// ─────────────────────────────────────────────
	// acceptApplication — 펀딩 달성 → Spot 자동 전환
	// ─────────────────────────────────────────────

	@Test
	@DisplayName("성공: 수락 후 펀딩 목표 달성이면 convertToSpot이 호출된다.")
	void acceptApplication_Success_SpotConversion() {
		// 목표 25000, 현재 20000 → 수락 1건(5000) 후 25000 = 달성
		FeedItem feedItem = feedItemWithPost("feed-001", "author-id", "post-001",
				FeedItemStatus.OPEN, 5000, 25000, 20000);
		FeedApplication application = appliedApplication("app-001", "feed-001");

		given(feedItemRepository.findByIdAndDeletedFalse("feed-001")).willReturn(Optional.of(feedItem));
		given(feedApplicationRepository.findByIdAndFeedItemId("app-001", "feed-001"))
				.willReturn(Optional.of(application));

		feedItemService.acceptApplication("feed-001", "app-001", "author-id");

		verify(postService, times(1)).convertToSpot("post-001");
		assertTrue(feedItem.isDeleted()); // 스팟 전환 후 피드 소프트 딜리트
	}

	@Test
	@DisplayName("실패: 작성자가 아닌 사람이 수락 시 예외 발생.")
	void acceptApplication_Fail_NotAuthor() {
		FeedItem feedItem = feedItem("feed-001", "author-id", FeedItemStatus.OPEN, 5000, 25000, 0);

		given(feedItemRepository.findByIdAndDeletedFalse("feed-001")).willReturn(Optional.of(feedItem));

		assertThrows(IllegalStateException.class,
				() -> feedItemService.acceptApplication("feed-001", "app-001", "other-user"));
	}

	// ─────────────────────────────────────────────
	// cancelApplication
	// ─────────────────────────────────────────────

	@Test
	@DisplayName("성공: APPLIED 상태 신청을 취소하면 CANCELLED로 변경된다.")
	void cancelApplication_Success() {
		FeedApplication application = appliedApplication("app-001", "feed-001");

		given(feedApplicationRepository.findByFeedItemIdAndUserIdAndStatus(
				"feed-001", "user-001", FeedApplicationStatus.APPLIED))
				.willReturn(Optional.of(application));

		feedItemService.cancelApplication("feed-001", "user-001");

		assertEquals(FeedApplicationStatus.CANCELLED, application.getStatus());
	}

	@Test
	@DisplayName("실패: 취소할 신청이 없으면 예외 발생.")
	void cancelApplication_Fail_NotFound() {
		given(feedApplicationRepository.findByFeedItemIdAndUserIdAndStatus(
				"feed-001", "user-001", FeedApplicationStatus.APPLIED))
				.willReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class,
				() -> feedItemService.cancelApplication("feed-001", "user-001"));
	}

	// ─────────────────────────────────────────────
	// 헬퍼 메서드
	// ─────────────────────────────────────────────

	private FeedItem feedItem(String id, String authorId, FeedItemStatus status,
			int price, int fundingGoal, int fundedAmount) {
		return FeedItem.builder()
				.id(id)
				.authorId(authorId)
				.title("테스트 피드")
				.location("서울")
				.authorNickname("테스터")
				.price(price)
				.type(PostType.OFFER)
				.status(status)
				.fundingGoal(fundingGoal)
				.fundedAmount(fundedAmount)
				.build();
	}

	private FeedItem feedItemWithPost(String id, String authorId, String postId,
			FeedItemStatus status, int price, int fundingGoal, int fundedAmount) {
		return FeedItem.builder()
				.id(id)
				.postId(postId)
				.authorId(authorId)
				.title("테스트 피드")
				.location("서울")
				.authorNickname("테스터")
				.price(price)
				.type(PostType.OFFER)
				.status(status)
				.fundingGoal(fundingGoal)
				.fundedAmount(fundedAmount)
				.build();
	}

	private FeedApplication appliedApplication(String id, String feedItemId) {
		return FeedApplication.builder()
				.id(id)
				.feedItemId(feedItemId)
				.userId("user-001")
				.userNickname("테스터")
				.proposal("신청합니다.")
				.build();
	}
}
