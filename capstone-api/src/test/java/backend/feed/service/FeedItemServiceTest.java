package backend.feed.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import backend.chat.service.ChatService;
import backend.feed.dto.FeedApplicationResponse;
import backend.feed.dto.FeedApplyRequest;
import backend.feed.dto.FeedItemResponse;
import backend.feed.entity.FeedApplication;
import backend.feed.entity.FeedApplicationRole;
import backend.feed.entity.FeedApplicationStatus;
import backend.feed.entity.FeedItem;
import backend.feed.repository.FeedApplicationRepository;
import backend.feed.repository.FeedItemRepository;
import backend.global.enums.FeedItemStatus;
import backend.global.enums.FeedType;
import backend.notification.service.NotificationService;
import backend.spot.entity.Spot;
import backend.spot.entity.SpotParticipant;
import backend.spot.repository.SpotParticipantRepository;
import backend.spot.repository.SpotRepository;

@ExtendWith(MockitoExtension.class)
class FeedItemServiceTest {

	@Mock
	private FeedItemRepository feedItemRepository;

	@Mock
	private FeedApplicationRepository feedApplicationRepository;

	@Mock
	private SpotRepository spotRepository;

	@Mock
	private SpotParticipantRepository spotParticipantRepository;

	@Mock
	private NotificationService notificationService;

	@Mock
	private ChatService chatService;

	@InjectMocks
	private FeedItemService feedItemService;

	@Captor
	private ArgumentCaptor<Spot> spotCaptor;

	@Captor
	private ArgumentCaptor<FeedApplication> feedApplicationCaptor;

	@Captor
	private ArgumentCaptor<List<SpotParticipant>> spotParticipantsCaptor;

	// ─────────────────────────────────────────────
	// getFeedItem
	// ─────────────────────────────────────────────

	@Test
	@DisplayName("성공: 존재하는 피드 ID로 조회하면 응답이 반환된다.")
	void getFeedItem_Success() {
		FeedItem feedItem = feedItem(1L, "dummy-author", FeedItemStatus.OPEN, 5000, 25000, 20000);

		given(feedItemRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feedItem));

		FeedItemResponse response = feedItemService.getFeedItem(1L);

		assertNotNull(response);
		assertEquals(1L, response.getId());
	}

	@Test
	@DisplayName("실패: 존재하지 않는 피드 ID 조회 시 예외 발생.")
	void getFeedItem_Fail_NotFound() {
		given(feedItemRepository.findByIdAndDeletedFalse(99L)).willReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class, () -> feedItemService.getFeedItem(99L));
	}

	// ─────────────────────────────────────────────
	// applyToFeed
	// ─────────────────────────────────────────────

	@Test
	@DisplayName("성공: OPEN 상태 피드에 신청하면 FeedApplication이 저장되고 role이 보존된다.")
	void applyToFeed_Success() {
		FeedItem feedItem = feedItem(1L, "author-id", FeedItemStatus.OPEN, 5000, 25000, 0);
		FeedApplyRequest request = new FeedApplyRequest("저는 경험이 있습니다.", FeedApplicationRole.PARTNER, 0);

		FeedApplication saved = FeedApplication.builder()
				.id("app-001")
				.feedItemId(1L)
				.userId("user-001")
				.userNickname("테스터")
				.proposal(request.getProposal())
				.appliedRole(request.getRole())
				.deposit(request.getDeposit())
				.build();

		given(feedItemRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feedItem));
		given(feedApplicationRepository.findByFeedItemIdAndUserIdAndStatus(
				1L, "user-001", FeedApplicationStatus.APPLIED)).willReturn(Optional.empty());
		given(feedApplicationRepository.save(any(FeedApplication.class))).willReturn(saved);

		FeedApplicationResponse response = feedItemService.applyToFeed(
				1L, "user-001", "테스터", request);

		assertNotNull(response);
		assertEquals(FeedApplicationStatus.APPLIED, response.getStatus());

		// role이 save에 그대로 전달되었는지 검증
		verify(feedApplicationRepository, times(1)).save(feedApplicationCaptor.capture());
		assertEquals(FeedApplicationRole.PARTNER, feedApplicationCaptor.getValue().getAppliedRole());
	}

	@Test
	@DisplayName("실패: 이미 신청한 피드에 중복 신청 시 예외 발생.")
	void applyToFeed_Fail_AlreadyApplied() {
		FeedItem feedItem = feedItem(1L, "author-id", FeedItemStatus.OPEN, 5000, 25000, 0);
		FeedApplication existing = FeedApplication.builder()
				.id("app-000")
				.feedItemId(1L)
				.userId("user-001")
				.userNickname("테스터")
				.build();

		given(feedItemRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feedItem));
		given(feedApplicationRepository.findByFeedItemIdAndUserIdAndStatus(
				1L, "user-001", FeedApplicationStatus.APPLIED)).willReturn(Optional.of(existing));

		assertThrows(IllegalStateException.class,
				() -> feedItemService.applyToFeed(1L, "user-001", "테스터",
						new FeedApplyRequest("신청", FeedApplicationRole.PARTNER, 0)));
	}

	@Test
	@DisplayName("실패: OPEN이 아닌 피드에 신청 시 예외 발생.")
	void applyToFeed_Fail_NotOpen() {
		FeedItem feedItem = feedItem(1L, "author-id", FeedItemStatus.MATCHED, 5000, 25000, 25000);

		given(feedItemRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feedItem));

		assertThrows(IllegalStateException.class,
				() -> feedItemService.applyToFeed(1L, "user-001", "테스터",
						new FeedApplyRequest("신청", FeedApplicationRole.PARTNER, 0)));
	}

	// ─────────────────────────────────────────────
	// acceptApplication — 펀딩 미달성
	// ─────────────────────────────────────────────

	@Test
	@DisplayName("성공: 수락 후 펀딩 목표 미달성이면 Spot 전환이 발생하지 않는다.")
	void acceptApplication_Success_FundingNotMet() {
		// 목표 25000, 현재 0 → 수락 1건(5000) 후 5000 → 아직 미달성
		FeedItem feedItem = feedItem(1L, "author-id", FeedItemStatus.OPEN, 5000, 25000, 0);
		FeedApplication application = appliedApplication("app-001", 1L);

		given(feedItemRepository.findByIdAndDeletedFalseForUpdate(1L)).willReturn(Optional.of(feedItem));
		given(feedApplicationRepository.findByIdAndFeedItemId("app-001", 1L))
				.willReturn(Optional.of(application));

		FeedApplicationResponse response = feedItemService.acceptApplication(
				1L, "app-001", "author-id");

		assertEquals(FeedApplicationStatus.ACCEPTED, response.getStatus());
		verify(spotRepository, never()).save(any(Spot.class));
		verify(notificationService, never()).send(anyString(), anyString());
	}

	// ─────────────────────────────────────────────
	// acceptApplication — 펀딩 달성 → Spot 자동 전환
	// ─────────────────────────────────────────────

	@Test
	@DisplayName("성공: 수락 후 매칭 조건 충족이면 Spot이 저장되고 피드 정보가 복사된다.")
	void acceptApplication_Success_SpotConversion() {
		// authorRole=SUPPORTER 인 OFFER 피드: 작성자가 SUPPORTER 슬롯 1개 차지.
		// maxParticipants=1, 신청자가 PARTNER로 수락되면 partners=1=maxParticipants 충족 → 자동 매칭.
		FeedItem feedItem = matchableFeedItem(1L, "author-id");
		FeedApplication application = partnerApplication("app-001", 1L);

		given(feedItemRepository.findByIdAndDeletedFalseForUpdate(1L)).willReturn(Optional.of(feedItem));
		given(feedApplicationRepository.findByIdAndFeedItemId("app-001", 1L))
				.willReturn(Optional.of(application));
		given(feedApplicationRepository.findAllByFeedItemIdAndStatus(1L, FeedApplicationStatus.ACCEPTED))
				.willReturn(List.of(application));
		given(spotRepository.save(any(Spot.class))).willAnswer(inv -> {
			Spot spot = inv.getArgument(0);
			ReflectionTestUtils.setField(spot, "id", 77L);
			return spot;
		});

		FeedApplicationResponse response = feedItemService.acceptApplication(1L, "app-001", "author-id");

		// Spot 저장 확인 + 피드 필드가 Spot에 정확히 복사되었는지 검증
		verify(spotRepository, times(1)).save(spotCaptor.capture());
		Spot savedSpot = spotCaptor.getValue();
		assertEquals("테스트 피드", savedSpot.getTitle());
		assertEquals("author-id", savedSpot.getAuthorId());
		assertEquals(FeedItemStatus.MATCHED, savedSpot.getStatus());

		// 피드 소프트 딜리트 확인
		assertTrue(feedItem.isDeleted());
		assertEquals(FeedItemStatus.MATCHED, feedItem.getStatus());
		assertEquals(77L, feedItem.getSpotId());
		assertTrue(response.isSpotConverted());
		assertEquals(77L, response.getSpotId());

		// 알림 발송 확인 (PR #99에서 send → sendAfterCommit 으로 전환됨)
		verify(notificationService, times(1)).sendAfterCommit(eq("author-id"), anyString());

		// chatService.linkGroupRoomToSpot 호출 확인
		// spot.getId()가 테스트 환경에서 null이므로 두 번째 인자는 anyString()으로 검증
		// 4번째 인자(allMemberIds: Collection)까지 시그니처에 맞춰 검증
		verify(chatService, times(1)).linkGroupRoomToSpot(eq("1"), anyString(), any(), any());

		verify(spotParticipantRepository, times(1)).saveAll(spotParticipantsCaptor.capture());
		SpotParticipant participant = spotParticipantsCaptor.getValue().stream()
				.filter(saved -> "user-001".equals(saved.getUserId()))
				.findFirst()
				.orElseThrow();
		assertEquals(FeedApplicationRole.PARTNER, participant.getApplicationRole());
	}

	@Test
	@DisplayName("실패: appliedRole이 null인 신청은 수락 거부.")
	void acceptApplication_Fail_NullRole() {
		FeedItem feedItem = feedItem(1L, "author-id", FeedItemStatus.OPEN, 5000, 25000, 0);
		FeedApplication application = FeedApplication.builder()
				.id("app-001")
				.feedItemId(1L)
				.userId("user-001")
				.userNickname("테스터")
				.proposal("신청")
				.build(); // appliedRole 미설정 — legacy/malformed row 시뮬레이션

		given(feedItemRepository.findByIdAndDeletedFalseForUpdate(1L)).willReturn(Optional.of(feedItem));
		given(feedApplicationRepository.findByIdAndFeedItemId("app-001", 1L))
				.willReturn(Optional.of(application));

		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> feedItemService.acceptApplication(1L, "app-001", "author-id"));
		assertTrue(ex.getMessage().contains("지원 역할"));
	}

	@Test
	@DisplayName("실패: 작성자가 아닌 사람이 수락 시 예외 발생.")
	void acceptApplication_Fail_NotAuthor() {
		FeedItem feedItem = feedItem(1L, "author-id", FeedItemStatus.OPEN, 5000, 25000, 0);

		given(feedItemRepository.findByIdAndDeletedFalseForUpdate(1L)).willReturn(Optional.of(feedItem));

		assertThrows(IllegalStateException.class,
				() -> feedItemService.acceptApplication(1L, "app-001", "other-user"));
	}

	// ─────────────────────────────────────────────
	// cancelApplication
	// ─────────────────────────────────────────────

	@Test
	@DisplayName("성공: APPLIED 상태 신청을 취소하면 CANCELLED로 변경된다.")
	void cancelApplication_Success() {
		FeedApplication application = appliedApplication("app-001", 1L);

		given(feedApplicationRepository.findByFeedItemIdAndUserIdAndStatus(
				1L, "user-001", FeedApplicationStatus.APPLIED))
				.willReturn(Optional.of(application));

		feedItemService.cancelApplication(1L, "user-001");

		assertEquals(FeedApplicationStatus.CANCELLED, application.getStatus());
	}

	@Test
	@DisplayName("실패: 취소할 신청이 없으면 예외 발생.")
	void cancelApplication_Fail_NotFound() {
		given(feedApplicationRepository.findByFeedItemIdAndUserIdAndStatus(
				1L, "user-001", FeedApplicationStatus.APPLIED))
				.willReturn(Optional.empty());

		assertThrows(IllegalArgumentException.class,
				() -> feedItemService.cancelApplication(1L, "user-001"));
	}

	// ─────────────────────────────────────────────
	// 헬퍼 메서드
	// ─────────────────────────────────────────────

	private FeedItem feedItem(Long id, String authorId, FeedItemStatus status,
			int price, int fundingGoal, int fundedAmount) {
		return FeedItem.builder()
				.id(id)
				.authorId(authorId)
				.title("테스트 피드")
				.location("서울")
				.authorNickname("테스터")
				.price(price)
				.type(FeedType.OFFER)
				.status(status)
				.fundingGoal(fundingGoal)
				.fundedAmount(fundedAmount)
				.build();
	}

	private FeedApplication appliedApplication(String id, Long feedItemId) {
		return FeedApplication.builder()
				.id(id)
				.feedItemId(feedItemId)
				.userId("user-001")
				.userNickname("테스터")
				.proposal("신청합니다.")
				.appliedRole(FeedApplicationRole.SUPPORTER)
				.build();
	}

	/**
	 * 단일 PARTNER 수락만으로 매칭 조건을 충족하도록 셋업된 피드.
	 * authorRole=SUPPORTER → 작성자가 SUPPORTER 슬롯 차지, maxParticipants=1.
	 */
	private FeedItem matchableFeedItem(Long id, String authorId) {
		return FeedItem.builder()
				.id(id)
				.authorId(authorId)
				.title("테스트 피드")
				.location("서울")
				.authorNickname("테스터")
				.price(5000)
				.type(FeedType.OFFER)
				.status(FeedItemStatus.OPEN)
				.authorRole(backend.global.enums.FeedAuthorRole.SUPPORTER)
				.maxParticipants(1)
				.fundingGoal(5000)
				.fundedAmount(0)
				.build();
	}

	private FeedApplication partnerApplication(String id, Long feedItemId) {
		return FeedApplication.builder()
				.id(id)
				.feedItemId(feedItemId)
				.userId("user-001")
				.userNickname("테스터")
				.proposal("신청합니다.")
				.appliedRole(FeedApplicationRole.PARTNER)
				.build();
	}
}
