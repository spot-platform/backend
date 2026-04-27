package backend.spot.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "spot_vote_options")
public class SpotVoteOption {

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

	/**
	 * 투표 선택 시 득표 수를 1 증가합니다.
	 */
	public void incrementCount() {
		this.voteCount++;
	}
}
