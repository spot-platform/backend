package backend.chat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import backend.chat.entity.ChatVoteOption;

public interface ChatVoteOptionRepository extends JpaRepository<ChatVoteOption, Long> {

	List<ChatVoteOption> findByVoteId(Long voteId);

	@Modifying
	@Query("update ChatVoteOption o set o.voteCount = o.voteCount + 1 where o.id = :id")
	void incrementVoteCount(@Param("id") Long id);

	@Modifying
	@Query("update ChatVoteOption o set o.voteCount = o.voteCount - 1 where o.id = :id and o.voteCount > 0")
	void decrementVoteCount(@Param("id") Long id);
}
