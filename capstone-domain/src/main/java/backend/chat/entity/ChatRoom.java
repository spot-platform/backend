package backend.chat.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
@Table(name = "chat_rooms")
public class ChatRoom {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "spot_id")
	private String spotId;

	@Column(name = "post_id")
	private String postId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private ChatRoomType type;

	@Column(name = "name")
	private String name;

	@Column(name = "image_url")
	private String imageUrl;

	@Column(name = "is_deleted", nullable = false)
	@Builder.Default
	private boolean isDeleted = false;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public void markDeleted() {
		this.isDeleted = true;
	}

	/** Feed → Spot 전환 시 호출. 채팅 내역을 유지하면서 spotId를 연결한다. */
	public void linkSpot(String spotId) {
		if (this.spotId != null && !this.spotId.equals(spotId)) {
			throw new IllegalStateException(
				"채팅방이 이미 다른 Spot(" + this.spotId + ")에 연결되어 있습니다.");
		}
		this.spotId = spotId;
	}
}
