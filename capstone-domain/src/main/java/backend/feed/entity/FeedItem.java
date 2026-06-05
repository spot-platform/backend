package backend.feed.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import backend.global.enums.FeedAuthorRole;
import backend.global.enums.FeedCategory;
import backend.global.enums.FeedItemStatus;
import backend.global.enums.FeedType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(name = "feed_items")
public class FeedItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column
	private String authorId;

	@Column(nullable = false)
	private String title;

	@Column(columnDefinition = "TEXT")
	private String description;

	@Column(nullable = false)
	private String location;

	@Column(nullable = false)
	private String authorNickname;

	@Column(nullable = false)
	private Integer price;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private FeedType type;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private FeedItemStatus status;

	@Enumerated(EnumType.STRING)
	@Column
	private FeedCategory category;

	@Column
	private Integer fundingGoal;

	@Builder.Default
	@Column(nullable = false)
	private Integer fundedAmount = 0;

	@Column
	private Integer maxParticipants;

	@Builder.Default
	@Column(nullable = false)
	private Integer confirmedPartnerCount = 0;

	@Builder.Default
	@Column(nullable = false, columnDefinition = "integer NOT NULL DEFAULT 0")
	private Integer confirmedSupporterCount = 0;

	@Builder.Default
	@Column(nullable = false)
	private Integer views = 0;

	@Builder.Default
	@Column(nullable = false)
	private Integer likes = 0;

	@Column
	private String deadline;

	@Builder.Default
	@Column(nullable = false, columnDefinition = "boolean NOT NULL DEFAULT false")
	private boolean deadlineNotifySent = false;

	@Column(length = 2048)
	private String imageUrl;

	@Column(length = 2048)
	private String authorAvatarUrl;

	@Enumerated(EnumType.STRING)
	@Column
	private FeedAuthorRole authorRole;

	@Column
	private Float authorRating;

	@Column
	private String authorField;

	@Column
	private Double lat;

	@Column
	private Double lng;

	/**
	 * AI 피드(시뮬레이션) 전용 참조 컬럼.
	 * 일반 사용자가 생성한 피드가 펀딩 목표를 달성해 Spot으로 전환될 때는
	 * 이 컬럼을 채우지 않는다. 전환 후 FeedItem은 소프트 딜리트 처리된다.
	 */
	@Column
	private Long spotId;

	@Builder.Default
	@Column(columnDefinition = "BOOLEAN NOT NULL DEFAULT FALSE")
	private boolean isAi = false;

	@Column(columnDefinition = "TEXT")
	private String planJson;

	@Column(columnDefinition = "TEXT")
	private String priceBreakdownJson;

	@Column(columnDefinition = "TEXT")
	private String preparationJson;

	@Column(columnDefinition = "TEXT")
	private String venueAnchorsJson;

	@Column(columnDefinition = "TEXT")
	private String primaryPinJson;

	@Column
	private String spotName;

	@Column(columnDefinition = "TEXT")
	private String detailDescription;

	@Column(length = 2048)
	private String supporterPhotoUrl;

	@Column(length = 2048)
	private String serviceStylePhotoUrl;

	@Column(columnDefinition = "TEXT")
	private String categoriesJson;

	@Column(columnDefinition = "TEXT")
	private String photoUrlsJson;

	@Builder.Default
	@Column(nullable = false, columnDefinition = "boolean NOT NULL DEFAULT false")
	private boolean earlyStartRequested = false;

	@Builder.Default
	@Column(name = "is_deleted", nullable = false)
	private boolean deleted = false;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	public void softDelete() {
		this.deleted = true;
	}

	public void convertToSpot(Long spotId) {
		this.spotId = spotId;
		this.status = FeedItemStatus.MATCHED;
		this.deleted = true;
	}

	/**
	 * 피드 당 수락 가능한 서포터 최대 인원.
	 * OFFER/REQUEST 모두 서포터는 1명으로 고정하는 것이 현재 서비스 정책이다.
	 * {@code maxParticipants} 필드는 프론트 UI 표시용(모집 정원 안내)이며,
	 * 수락 상한 판단에는 사용하지 않는다.
	 */
	private static final int MAX_SUPPORTERS_PER_FEED = 1;

	/** 작성자가 SUPPORTER 역할이면 1, 아니면 0. 외부 SUPPORTER 슬롯 카운트에 합산. */
	private int authorSupporterSlot() {
		return this.authorRole == FeedAuthorRole.SUPPORTER ? 1 : 0;
	}

	/** 작성자가 PARTNER 역할이면 1, 아니면 0. 외부 PARTNER 카운트에 합산. */
	private int authorPartnerSlot() {
		return this.authorRole == FeedAuthorRole.PARTNER ? 1 : 0;
	}

	private int safeConfirmedSupporterCount() {
		return this.confirmedSupporterCount != null ? this.confirmedSupporterCount : 0;
	}

	private int safeConfirmedPartnerCount() {
		return this.confirmedPartnerCount != null ? this.confirmedPartnerCount : 0;
	}

	/**
	 * 서포터 추가 수락 가능 여부. 서포터는 1명으로 고정이며,
	 * 작성자 본인이 SUPPORTER 역할이면 이미 한 슬롯을 차지한 것으로 계산한다.
	 */
	public boolean canAcceptMoreSupporters() {
		return (safeConfirmedSupporterCount() + authorSupporterSlot()) < MAX_SUPPORTERS_PER_FEED;
	}

	public void recordSupporterAccepted() {
		this.confirmedSupporterCount = safeConfirmedSupporterCount() + 1;
	}

	public void recordPartnerAccepted() {
		this.confirmedPartnerCount = safeConfirmedPartnerCount() + 1;
	}

	/**
	 * 자동 Spot 전환 조건: 서포터 ≥ 1 + 파트너 ≥ {@code maxParticipants}.
	 *
	 * <p>{@code maxParticipants} 의미 (hoTan35 리뷰 반영):
	 * <b>작성자 본인 PARTNER 슬롯을 포함한 "총 파트너 수"</b>. 즉
	 * <ul>
	 *   <li>authorRole=SUPPORTER 인 피드: 외부 파트너 수가 maxParticipants 도달 시 충족</li>
	 *   <li>authorRole=PARTNER 인 피드: 외부 파트너 수 + 1(작성자) 가 maxParticipants 도달 시 충족</li>
	 * </ul>
	 * 두 케이스 모두 "확정된 파트너 헤드카운트가 maxParticipants 와 같거나 크다"로 일관.
	 * 작성자 본인이 SUPPORTER/PARTNER 역할이면 해당 슬롯에 합산해서 계산한다.
	 * maxParticipants 미설정 시 자동 전환 없음 — 작성자가 수동 진행 요청해야 함.
	 */
	public boolean isReadyToMatch() {
		if (this.maxParticipants == null) {
			return false;
		}
		int supporters = safeConfirmedSupporterCount() + authorSupporterSlot();
		int partners = safeConfirmedPartnerCount() + authorPartnerSlot();
		return supporters >= 1 && partners >= this.maxParticipants;
	}

	/**
	 * 조기 시작 요청 가능 조건: 서포터 1명 + 파트너 1명 이상.
	 * 작성자 본인 역할도 슬롯에 합산한다.
	 */
	public boolean canRequestEarlyStart() {
		int supporters = safeConfirmedSupporterCount() + authorSupporterSlot();
		int partners = safeConfirmedPartnerCount() + authorPartnerSlot();
		return supporters >= 1 && partners >= 1 && !this.earlyStartRequested;
	}

	public void requestEarlyStart() {
		this.earlyStartRequested = true;
	}

	public void markDeadlineNotifySent() {
		this.deadlineNotifySent = true;
	}

	/**
	 * 마감일이 변경될 때 알림 플래그를 초기화한다.
	 * TODO: deadline 변경 API가 추가되면 해당 Service 로직에서 이 메서드를 호출해야 함.
	 *       그렇지 않으면 변경된 새 마감일에 알림이 발송되지 않는다.
	 */
	public void resetDeadlineNotifySent() {
		this.deadlineNotifySent = false;
	}
}
