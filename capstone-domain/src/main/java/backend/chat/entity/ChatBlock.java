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
 * 사용자 간 차단 관계. (blocker → blocked) 단방향.
 *
 * <p>차단 적용 시점:
 * <ul>
 *   <li>차단 row 의 {@link #createdAt} **이후** 메시지만 가려진다 (이전 메시지는 그대로 보임).</li>
 *   <li>PERSONAL 채팅에만 적용. GROUP 은 다수 참여자가 봐야 하므로 차단 무시.</li>
 *   <li>차단 중인 동안 양방향 모두 PERSONAL 방을 새로 시작할 수 없다 (createPersonalRoom 시 양쪽 검증).</li>
 * </ul>
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
@Table(
	name = "chat_blocks",
	uniqueConstraints = @UniqueConstraint(
		name = "uq_chat_block_pair",
		columnNames = {"blocker_id", "blocked_id"}
	),
	indexes = {
		// "내가 차단한 목록" 조회 — 최신순
		@Index(name = "idx_chat_block_blocker", columnList = "blocker_id, created_at DESC"),
		// "나를 차단한 사람" 역조회 — createPersonalRoom 양방향 검증용
		@Index(name = "idx_chat_block_blocked", columnList = "blocked_id")
	}
)
public class ChatBlock {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "blocker_id", nullable = false)
	private String blockerId;

	@Column(name = "blocked_id", nullable = false)
	private String blockedId;

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private LocalDateTime createdAt;
}
