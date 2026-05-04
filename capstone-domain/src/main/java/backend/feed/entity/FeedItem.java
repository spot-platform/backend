package backend.feed.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import backend.global.enums.FeedCategory;
import backend.global.enums.FeedItemStatus;
import backend.global.enums.PostType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
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
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "uuid2")
	@Column(columnDefinition = "VARCHAR(36)")
	private String id;

	@Column
	private String postId;

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
	private PostType type;

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

	public void accumulateFunding(int amount) {
		this.fundedAmount += amount;
		this.confirmedPartnerCount += 1;
	}

	public boolean isFundingGoalMet() {
		if (this.fundingGoal == null || this.fundingGoal <= 0) {
			return false;
		}
		return this.fundedAmount >= this.fundingGoal;
	}
}
