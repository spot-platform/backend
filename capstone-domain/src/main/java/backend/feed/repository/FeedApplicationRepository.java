package backend.feed.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import backend.feed.entity.FeedApplication;
import backend.feed.entity.FeedApplicationStatus;

@Repository
public interface FeedApplicationRepository extends JpaRepository<FeedApplication, String> {

	Optional<FeedApplication> findByFeedItemIdAndUserIdAndStatus(
			String feedItemId, String userId, FeedApplicationStatus status);

	Optional<FeedApplication> findByIdAndFeedItemId(String id, String feedItemId);
}
