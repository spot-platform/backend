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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
	name = "chat_vote_answers",
	uniqueConstraints = @UniqueConstraint(
		name = "uq_chat_vote_user_option",
		columnNames = {"vote_id", "user_id", "option_id"}
	)
)
public class ChatVoteAnswer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "vote_id", nullable = false)
	private Long voteId;

	@Column(name = "option_id", nullable = false)
	private Long optionId;

	@Column(name = "user_id", nullable = false)
	private String userId;

	@CreatedDate
	@Column(name = "answered_at", nullable = false, updatable = false)
	private LocalDateTime answeredAt;
}
