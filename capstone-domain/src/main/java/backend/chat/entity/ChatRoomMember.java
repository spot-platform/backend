package backend.chat.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 채팅방 멤버십. (chat_room_id, user_id) 가 유니크.
 *
 * <p>PERSONAL 방은 정확히 2 명, GROUP 방은 N 명의 멤버를 가진다.
 * 멤버가 아닌 사용자는 해당 채팅방의 메시지 조회/전송이 불가능하다.
 * 가입 시점 이전 메시지도 가시 (Slack 스타일) — joinedAt 은 추후 정책 변경/unread 베이스라인 용도.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(
	name = "chat_room_members",
	uniqueConstraints = @UniqueConstraint(
		name = "uq_chat_room_member",
		columnNames = {"chat_room_id", "user_id"}
	),
	indexes = {
		@Index(name = "idx_chat_room_member_user", columnList = "user_id")
	}
)
public class ChatRoomMember {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "chat_room_id", nullable = false)
	private Long chatRoomId;

	@Column(name = "user_id", nullable = false)
	private String userId;

	@CreatedDate
	@Column(name = "joined_at", nullable = false, updatable = false)
	private LocalDateTime joinedAt;

	/**
	 * 본 멤버가 마지막으로 읽은 메시지 ID. null = 한 번도 읽지 않음.
	 *
	 * <p>unreadCount 는 {@code COUNT(chat_messages WHERE room_id = ? AND id > lastReadMessageId)}
	 * 로 계산. 이 컬럼은 단조 증가만 허용한다 ({@link #markRead}).
	 */
	@Column(name = "last_read_message_id")
	private Long lastReadMessageId;

	/**
	 * 본 멤버의 이 방에 대한 알림 수신 여부. false = 음소거.
	 * 룸 스코프 알림(예: 투표 시작/마감)이 muted 멤버에게는 전송되지 않는다.
	 */
	@Builder.Default
	@Column(name = "notification_enabled", nullable = false)
	private boolean notificationEnabled = true;

	/** 방 알림 on/off 설정. */
	public void updateNotificationEnabled(boolean enabled) {
		this.notificationEnabled = enabled;
	}

	/**
	 * 단조 증가 read 마커 업데이트. 더 작은 ID 를 시도하면 무시되어 read 가 뒤로 가지 않는다.
	 * 멱등 — 같은 messageId 를 여러 번 호출해도 결과 동일.
	 *
	 * @return 실제로 진행된 경우 true (caller 가 SSE broadcast 등 사이드 이펙트를 분기할 수 있게)
	 */
	public boolean markRead(Long messageId) {
		if (messageId == null) {
			return false;
		}
		if (lastReadMessageId == null || messageId > lastReadMessageId) {
			this.lastReadMessageId = messageId;
			return true;
		}
		return false;
	}
}
