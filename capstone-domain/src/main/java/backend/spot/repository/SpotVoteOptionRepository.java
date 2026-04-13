package backend.spot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.spot.entity.SpotVoteOption;

public interface SpotVoteOptionRepository extends JpaRepository<SpotVoteOption, Long> {

	List<SpotVoteOption> findByVoteId(Long voteId);
}
