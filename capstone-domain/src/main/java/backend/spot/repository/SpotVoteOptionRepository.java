package backend.spot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import backend.spot.entity.SpotVoteOption;

public interface SpotVoteOptionRepository extends JpaRepository<SpotVoteOption, Long> {

	List<SpotVoteOption> findByVoteId(Long voteId);

	/**
	 * 득표 수를 DB 수준에서 원자적으로 1 증가시킵니다.
	 * read-modify-write 경쟁 조건 없이 동시 투표를 안전하게 처리합니다.
	 */
	@Modifying
	@Query("UPDATE SpotVoteOption o SET o.voteCount = o.voteCount + 1 WHERE o.id = :id")
	void incrementVoteCount(@Param("id") Long id);
}
