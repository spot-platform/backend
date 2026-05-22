package backend.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(
	name = "chat_vote_options",
	indexes = @Index(name = "idx_chat_vote_option_vote", columnList = "vote_id")
)
public class ChatVoteOption {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "vote_id", nullable = false)
	private Long voteId;

	@Column(nullable = false)
	private String content;

	@Builder.Default
	@Column(nullable = false)
	private Integer voteCount = 0;

	public void incrementCount() {
		this.voteCount++;
	}

	public void decrementCount() {
		if (this.voteCount > 0) {
			this.voteCount--;
		}
	}

	public void updateContent(String newContent) {
		this.content = newContent;
	}
}
