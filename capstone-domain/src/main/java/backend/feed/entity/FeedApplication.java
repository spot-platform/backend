package backend.feed.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
@Table(name = "feed_applications")
public class FeedApplication {

	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "uuid2")
	@Column(columnDefinition = "VARCHAR(36)")
	private String id;

	@Column(nullable = false)
	private String feedItemId;

	@Column(nullable = false)
	private String userId;

	@Column(nullable = false)
	private String userNickname;

	@Column(columnDefinition = "TEXT")
	private String proposal;

	@Enumerated(EnumType.STRING)
	@Builder.Default
	@Column(nullable = false)
	private FeedApplicationStatus status = FeedApplicationStatus.APPLIED;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public void accept() {
		if (this.status != FeedApplicationStatus.APPLIED) {
			throw new IllegalStateException("신청 상태인 경우만 수락할 수 있습니다.");
		}
		this.status = FeedApplicationStatus.ACCEPTED;
	}

	public void reject() {
		if (this.status != FeedApplicationStatus.APPLIED) {
			throw new IllegalStateException("신청 상태인 경우만 거절할 수 있습니다.");
		}
		this.status = FeedApplicationStatus.REJECTED;
	}

	public void cancel() {
		if (this.status != FeedApplicationStatus.APPLIED) {
			throw new IllegalStateException("신청 상태인 경우만 취소할 수 있습니다.");
		}
		this.status = FeedApplicationStatus.CANCELLED;
	}
}
