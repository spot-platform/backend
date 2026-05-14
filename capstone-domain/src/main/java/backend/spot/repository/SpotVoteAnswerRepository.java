package backend.spot.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.spot.entity.SpotVoteAnswer;

public interface SpotVoteAnswerRepository extends JpaRepository<SpotVoteAnswer, Long> {

	List<SpotVoteAnswer> findByVoteId(Long voteId);

	List<SpotVoteAnswer> findAllByVoteIdAndUserId(Long voteId, String userId);

	Optional<SpotVoteAnswer> findByVoteIdAndUserId(Long voteId, String userId);

	boolean existsByVoteIdAndUserId(Long voteId, String userId);

	/**
	 * 특정 유저의 특정 투표 답변을 일괄 삭제합니다.
	 * 단일선택 모드에서 표를 변경할 때 이전 답변을 제거하는 용도입니다.
	 */
	void deleteAllByVoteIdAndUserId(Long voteId, String userId);
}
