package backend.chat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.chat.entity.ChatVoteAnswer;

public interface ChatVoteAnswerRepository extends JpaRepository<ChatVoteAnswer, Long> {

	List<ChatVoteAnswer> findByVoteId(Long voteId);

	List<ChatVoteAnswer> findByVoteIdAndUserId(Long voteId, String userId);

	Optional<ChatVoteAnswer> findByVoteIdAndUserIdAndOptionId(Long voteId, String userId, Long optionId);

	void deleteByVoteIdAndUserId(Long voteId, String userId);

	void deleteByOptionId(Long optionId);
}
