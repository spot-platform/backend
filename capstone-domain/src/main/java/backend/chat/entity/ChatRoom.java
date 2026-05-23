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
import jakarta.persistence.Index;
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
@Table(
	name = "chat_rooms",
	indexes = {
		@Index(name = "idx_chat_rooms_canonical_pair", columnList = "canonical_pair")
	}
)
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

	/**
	 * PERSONAL 방 중복 방지용 정규화 키. LEAST(userA,userB) + ":" + GREATEST(userA,userB).
	 * GROUP 방은 null. DB 레벨 partial unique index (type='PERSONAL' AND is_deleted=false) 와 짝.
	 */
	@Column(name = "canonical_pair")
	private String canonicalPair;

	/**
	 * 스팟 취소/완료 이후 더 이상 메시지를 보낼 수 없는 상태.
	 * sendMessage 진입 시 검증.
	 */
	@Column(name = "is_read_only", nullable = false, columnDefinition = "boolean default false")
	@Builder.Default
	private boolean isReadOnly = false;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	/**
	 * 마지막 메시지 시점. 채팅방 목록 최신순 정렬 기준.
	 * sendMessage() 마다 touch() 로 갱신.
	 */
	@Column(name = "updated_at")
	private LocalDateTime updatedAt;

	public void markDeleted() {
		this.isDeleted = true;
	}

	public void markReadOnly() {
		this.isReadOnly = true;
	}

	/** 메시지 전송 시 호출 — updatedAt 을 현재 시각으로 갱신한다. */
	public void touch() {
		this.updatedAt = LocalDateTime.now();
	}

	/** 채팅방 이름을 Feed/Spot 제목으로 갱신한다. */
	public void updateName(String name) {
		this.name = name;
	}

	/** Feed → Spot 전환 시 호출. 채팅 내역을 유지하면서 spotId를 연결한다. */
	public void linkSpot(String spotId) {
		if (this.spotId != null && !this.spotId.equals(spotId)) {
			throw new IllegalStateException(
				"채팅방이 이미 다른 Spot(" + this.spotId + ")에 연결되어 있습니다.");
		}
		this.spotId = spotId;
	}

	/**
	 * PERSONAL 방 생성 시 호출. (userA, userB) 순서 불문 항상 동일한 키를 생성한다.
	 * format: LEAST(a, b) + ":" + GREATEST(a, b)
	 */
	public static String buildCanonicalPair(String userA, String userB) {
		if (userA.compareTo(userB) <= 0) {
			return userA + ":" + userB;
		}
		return userB + ":" + userA;
	}
}
