package backend.spot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.spot.entity.SpotVote;
import backend.spot.entity.VoteState;

public interface SpotVoteRepository extends JpaRepository<SpotVote, Long> {

	List<SpotVote> findBySpotId(String spotId);

	List<SpotVote> findBySpotIdAndState(String spotId, VoteState state);

	List<SpotVote> findBySpotIdOrderByCreatedAtDesc(String spotId);
}
