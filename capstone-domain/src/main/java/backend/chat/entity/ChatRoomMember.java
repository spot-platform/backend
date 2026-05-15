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
}
