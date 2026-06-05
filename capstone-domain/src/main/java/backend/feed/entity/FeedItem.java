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
	@Column(nullable = false)
	private Integer views = 0;

	@Builder.Default
	@Column(nullable = false)
	private Integer likes = 0;

	@Column
	private String deadline;

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

	/**
	 * 추가 서포터를 수락할 수 있는지 확인한다.
	 * 수락 상한은 {@link #MAX_SUPPORTERS_PER_FEED}(현재 1명)으로 고정된다.
	 */
	public boolean canAcceptMore() {
		int confirmed = this.confirmedPartnerCount != null ? this.confirmedPartnerCount : 0;
		return confirmed < MAX_SUPPORTERS_PER_FEED;
	}

	public void accumulateFunding(int amount) {
		this.fundedAmount = (this.fundedAmount != null ? this.fundedAmount : 0) + amount;
		this.confirmedPartnerCount = (this.confirmedPartnerCount != null ? this.confirmedPartnerCount : 0) + 1;
	}

	public boolean isFundingGoalMet() {
		if (this.fundingGoal == null || this.fundingGoal <= 0) {
			return false;
		}
		return this.fundedAmount >= this.fundingGoal;
	}
}
