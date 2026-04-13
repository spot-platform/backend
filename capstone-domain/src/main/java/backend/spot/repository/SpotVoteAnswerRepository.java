package backend.spot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.spot.entity.SpotVoteAnswer;

public interface SpotVoteAnswerRepository extends JpaRepository<SpotVoteAnswer, Long> {

	List<SpotVoteAnswer> findByVoteId(Long voteId);

	Optional<SpotVoteAnswer> findByVoteIdAndUserId(Long voteId, String userId);

	boolean existsByVoteIdAndUserId(Long voteId, String userId);
}
