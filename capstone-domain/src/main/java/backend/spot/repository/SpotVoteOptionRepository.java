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

	/**
	 * 득표 수를 DB 수준에서 원자적으로 1 감소시킵니다.
	 * 단일선택 투표에서 표 변경 시 이전 옵션의 카운트를 되돌리는 용도이며,
	 * 음수가 되지 않도록 voteCount > 0 가드를 둡니다.
	 */
	@Modifying
	@Query("UPDATE SpotVoteOption o SET o.voteCount = o.voteCount - 1 WHERE o.id = :id AND o.voteCount > 0")
	void decrementVoteCount(@Param("id") Long id);
}
