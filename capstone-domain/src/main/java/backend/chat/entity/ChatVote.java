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

/**
 * 채팅방 투표. 스팟 투표와 달리 채팅방 단위로 생성되며, 익명 투표를 지원한다.
 *
 * <p>30분 전 마감 알림은 스케줄러가 {@code closedAt} 을 기준으로 주기적으로 체크한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(
	name = "chat_votes",
	indexes = @Index(name = "idx_chat_vote_room", columnList = "chat_room_id")
)
public class ChatVote {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "chat_room_id", nullable = false)
	private Long chatRoomId;

	@Column(name = "creator_id", nullable = false)
	private String creatorId;

	@Column(nullable = false)
	private String question;

	@Enumerated(EnumType.STRING)
	@Builder.Default
	@Column(nullable = false)
	private ChatVoteState state = ChatVoteState.ACTIVE;

	/** 다중선택 허용 여부. false = 단일선택. */
	@Builder.Default
	@Column(nullable = false)
	private boolean multiSelect = false;

	/** 익명 투표 여부. true 면 응답에서 voterIds 가 빈 리스트로 내려간다. */
	@Builder.Default
	@Column(nullable = false)
	private boolean anonymous = false;

	/** 자동 마감 일시. null 이면 수동 마감. */
	@Column
	private LocalDateTime closedAt;

	/** closedAt 30분 전 알림 발송 완료 여부. 중복 발송 방지. */
	@Builder.Default
	@Column(name = "notify_sent", nullable = false)
	private boolean notifySent = false;

	@CreatedDate
	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	public void close() {
		this.state = ChatVoteState.CLOSED;
		this.closedAt = LocalDateTime.now();
	}

	public void markNotifySent() {
		this.notifySent = true;
	}
}
