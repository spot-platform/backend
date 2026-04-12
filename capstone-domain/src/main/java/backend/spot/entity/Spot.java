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
