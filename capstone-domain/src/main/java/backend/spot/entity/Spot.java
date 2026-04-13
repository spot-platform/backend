package backend.spot.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import backend.global.enums.FeedItemStatus;
import backend.global.enums.PostType;
import backend.post.entity.Post;
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
@Table(name = "spots")
public class Spot {

	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "uuid2")
	@Column(columnDefinition = "VARCHAR(36)")
	private String id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PostType type; // OFFER, REQUEST

	@Enumerated(EnumType.STRING)
	@Builder.Default
	@Column(nullable = false)
	private FeedItemStatus status = FeedItemStatus.OPEN;

	@Column(nullable = false)
	private String title;

	@Column(columnDefinition = "TEXT", nullable = false)
	private String description;

	@Column(nullable = false)
	private Integer pointCost;

	@Column(nullable = false)
	private String authorId;

	@Column(nullable = false)
	private String authorNickname;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@LastModifiedDate
	@Column(nullable = false)
	private LocalDateTime updatedAt;

	/**
	 * 스팟 상태를 MATCHED로 변경 (OPEN → MATCHED)
	 * 참여자가 확정되어 스팟이 진행 중으로 전환될 때 호출
	 */
	public void match() {
		if (this.status != FeedItemStatus.OPEN) {
			throw new IllegalStateException("모집 중인 스팟만 매칭할 수 있습니다. 현재 상태: " + this.status);
		}
		this.status = FeedItemStatus.MATCHED;
	}

	/**
	 * 스팟 상태를 CLOSED로 변경 (OPEN → CLOSED)
	 * 모집 중 취소될 때 호출
	 */
	public void cancel() {
		if (this.status != FeedItemStatus.OPEN) {
			throw new IllegalStateException("모집 중인 스팟만 취소할 수 있습니다. 현재 상태: " + this.status);
		}
		this.status = FeedItemStatus.CLOSED;
	}

	/**
	 * 스팟 상태를 CLOSED로 변경 (MATCHED → CLOSED)
	 * 스팟 활동이 모두 완료될 때 호출
	 */
	public void complete() {
		if (this.status != FeedItemStatus.MATCHED) {
			throw new IllegalStateException("진행 중인 스팟만 완료할 수 있습니다. 현재 상태: " + this.status);
		}
		this.status = FeedItemStatus.CLOSED;
	}

	/**
	 * Post 데이터를 기반으로 Spot을 생성하는 정적 팩토리 메서드
	 */
	public static Spot fromPost(Post post, String title, String description, Integer pointCost) {
		return Spot.builder()
				.type(post.getType())
				.status(FeedItemStatus.MATCHED) // 매칭된 상태로 생성
				.title(title)
				.description(description)
				.pointCost(pointCost)
				.authorId(post.getAuthorId())
				.authorNickname(post.getAuthorNickname())
				.build();
	}
}
