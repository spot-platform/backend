package backend.spot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.spot.entity.SpotVote;
import backend.spot.entity.VoteState;

public interface SpotVoteRepository extends JpaRepository<SpotVote, Long> {

	List<SpotVote> findBySpotId(Long spotId);

	List<SpotVote> findBySpotIdAndState(Long spotId, VoteState state);

	List<SpotVote> findBySpotIdOrderByCreatedAtDesc(Long spotId);
}
