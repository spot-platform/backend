package backend.feed.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import backend.global.enums.FeedAuthorRole;
import backend.global.enums.FeedItemStatus;
import backend.global.enums.FeedType;

/**
 * PR #121 리뷰 반영 검증 — authorRole 슬롯이 supporter/partner 카운트에 반영되는지.
 *
 * 시나리오:
 *  1. authorRole=SUPPORTER 인 피드: 작성자가 이미 서포터 슬롯을 차지 → 외부 SUPPORTER 추가 수락 불가.
 *  2. authorRole=PARTNER 인 피드: 작성자가 파트너 슬롯 1개 차지 → 외부 파트너만으로 maxParticipants 충족 가능.
 *  3. authorRole=null (legacy): 기존 동작 그대로 — confirmedCount만으로 판정.
 */
class FeedItemSlotTest {

	private FeedItem feed(FeedAuthorRole authorRole, Integer maxParticipants,
			Integer confirmedSupporter, Integer confirmedPartner) {
		return FeedItem.builder()
			.title("t")
			.location("l")
			.authorId("u1")
			.authorNickname("작성자")
			.price(10000)
			.type(FeedType.OFFER)
			.status(FeedItemStatus.OPEN)
			.authorRole(authorRole)
			.maxParticipants(maxParticipants)
			.confirmedSupporterCount(confirmedSupporter)
			.confirmedPartnerCount(confirmedPartner)
			.build();
	}

	@Nested
	@DisplayName("canAcceptMoreSupporters")
	class CanAcceptMoreSupporters {

		@Test
		@DisplayName("authorRole=SUPPORTER 인 피드는 외부 SUPPORTER 수락 불가 (작성자가 슬롯 차지)")
		void authorIsSupporter_blocksExternalSupporter() {
			FeedItem f = feed(FeedAuthorRole.SUPPORTER, 2, 0, 0);
			assertFalse(f.canAcceptMoreSupporters());
		}

		@Test
		@DisplayName("authorRole=PARTNER 인 피드는 외부 SUPPORTER 1명 수락 가능")
		void authorIsPartner_allowsOneSupporter() {
			FeedItem f = feed(FeedAuthorRole.PARTNER, 2, 0, 0);
			assertTrue(f.canAcceptMoreSupporters());
		}

		@Test
		@DisplayName("authorRole=PARTNER 이미 1명 수락 후 추가 수락 불가")
		void authorIsPartner_capsAtOneSupporter() {
			FeedItem f = feed(FeedAuthorRole.PARTNER, 2, 1, 0);
			assertFalse(f.canAcceptMoreSupporters());
		}

		@Test
		@DisplayName("authorRole=null (legacy) 인 피드는 confirmedSupporterCount만 본다")
		void legacyBehavior_authorRoleNull() {
			FeedItem f = feed(null, 2, 0, 0);
			assertTrue(f.canAcceptMoreSupporters());
			FeedItem full = feed(null, 2, 1, 0);
			assertFalse(full.canAcceptMoreSupporters());
		}
	}

	@Nested
	@DisplayName("isReadyToMatch")
	class IsReadyToMatch {

		@Test
		@DisplayName("authorRole=SUPPORTER 인 피드는 파트너 maxParticipants 충족 시 자동 매칭")
		void authorSupporter_autoMatchOnPartnersOnly() {
			// 작성자가 SUPPORTER 1명 슬롯 차지 + 파트너 2명 수락 → maxParticipants=2 충족
			FeedItem f = feed(FeedAuthorRole.SUPPORTER, 2, 0, 2);
			assertTrue(f.isReadyToMatch());
		}

		@Test
		@DisplayName("authorRole=PARTNER 인 피드는 서포터 1명 + 파트너(작성자 포함) maxParticipants 충족 시 매칭")
		void authorPartner_autoMatchWithSupporter() {
			// 작성자가 PARTNER 슬롯 1개 차지 + 외부 SUPPORTER 1명 + 외부 PARTNER 1명 → maxParticipants=2 충족
			FeedItem f = feed(FeedAuthorRole.PARTNER, 2, 1, 1);
			assertTrue(f.isReadyToMatch());
		}

		@Test
		@DisplayName("authorRole=null (legacy) 인 피드는 confirmed 카운트만으로 판정")
		void legacyBehavior_authorRoleNull() {
			FeedItem f = feed(null, 2, 0, 2);
			assertFalse(f.isReadyToMatch(), "서포터 0이면 매칭 안됨");
			FeedItem g = feed(null, 2, 1, 2);
			assertTrue(g.isReadyToMatch());
		}

		@Test
		@DisplayName("maxParticipants=null 이면 자동 전환 없음")
		void noMaxParticipants_noAutoMatch() {
			FeedItem f = feed(FeedAuthorRole.SUPPORTER, null, 0, 5);
			assertFalse(f.isReadyToMatch());
		}
	}

	@Nested
	@DisplayName("canRequestEarlyStart")
	class CanRequestEarlyStart {

		@Test
		@DisplayName("authorRole=SUPPORTER + 파트너 1명 수락 시 조기 시작 요청 가능")
		void authorSupporter_partnerOne_canRequest() {
			FeedItem f = feed(FeedAuthorRole.SUPPORTER, 5, 0, 1);
			assertTrue(f.canRequestEarlyStart());
		}

		@Test
		@DisplayName("authorRole=PARTNER + 서포터 1명 수락 시 조기 시작 요청 가능")
		void authorPartner_supporterOne_canRequest() {
			FeedItem f = feed(FeedAuthorRole.PARTNER, 5, 1, 0);
			assertTrue(f.canRequestEarlyStart());
		}

		@Test
		@DisplayName("아무도 수락 안한 상태에선 조기 시작 불가")
		void noAcceptance_cannotRequest() {
			FeedItem f = feed(FeedAuthorRole.SUPPORTER, 5, 0, 0);
			assertFalse(f.canRequestEarlyStart());
		}
	}

	@Nested
	@DisplayName("record* 메서드 누적 동작")
	class RecordMethods {

		@Test
		@DisplayName("recordSupporterAccepted 호출 시 confirmedSupporterCount +1")
		void recordSupporter() {
			FeedItem f = feed(null, 2, 0, 0);
			f.recordSupporterAccepted();
			assertEquals(1, f.getConfirmedSupporterCount());
		}

		@Test
		@DisplayName("recordPartnerAccepted 호출 시 confirmedPartnerCount +1")
		void recordPartner() {
			FeedItem f = feed(null, 2, 0, 0);
			f.recordPartnerAccepted();
			assertEquals(1, f.getConfirmedPartnerCount());
		}
	}
}
