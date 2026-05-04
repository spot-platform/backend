package backend.post.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import backend.global.enums.FeedItemStatus;
import backend.global.enums.PostSpotCategory;
import backend.global.enums.PostType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
@Table(name = "posts")
public class Post {

	@Id
	@GeneratedValue(generator = "uuid2")
	@GenericGenerator(name = "uuid2", strategy = "uuid2")
	@Column(columnDefinition = "VARCHAR(36)")
	private String id;

	@Column
	private String feedItemId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private PostType type;

	@Enumerated(EnumType.STRING)
	@Builder.Default
	@Column(nullable = false)
	private FeedItemStatus status = FeedItemStatus.OPEN;

	@Column(nullable = false)
	private String authorId;

	@Column(nullable = false)
	private String authorNickname;

	@Column(nullable = false)
	private String spotName;

	@Column(nullable = false)
	private String title;

	@Column(columnDefinition = "TEXT", nullable = false)
	private String content;

	@Column(nullable = false)
	private Integer pointCost;

	@Column
	private String location;

	@Column
	private String deadline;

	@Column
	private Integer desiredPrice;

	@Column
	private Integer maxPartnerCount;

	@Column(columnDefinition = "TEXT")
	private String detailDescription;

	@ElementCollection
	@CollectionTable(name = "post_categories", joinColumns = @JoinColumn(name = "post_id"))
	@Enumerated(EnumType.STRING)
	@Column(name = "category")
	@Builder.Default
	private List<PostSpotCategory> categories = new ArrayList<>();

	@ElementCollection
	@CollectionTable(name = "post_photos", joinColumns = @JoinColumn(name = "post_id"))
	@Column(name = "photo_url")
	@Builder.Default
	private List<String> photoUrls = new ArrayList<>();

	private String supporterPhotoUrl;

	private String serviceStylePhotoUrl;

	@Builder.Default
	@Column(name = "is_deleted", nullable = false)
	private boolean deleted = false;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public void match() {
		this.status = FeedItemStatus.MATCHED;
	}

	public void softDelete() {
		this.deleted = true;
	}

	public void linkFeedItem(String feedItemId) {
		this.feedItemId = feedItemId;
	}
}
