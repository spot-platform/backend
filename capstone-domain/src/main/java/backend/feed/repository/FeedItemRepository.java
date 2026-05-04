package backend.feed.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import backend.feed.entity.FeedItem;

@Repository
public interface FeedItemRepository extends JpaRepository<FeedItem, String>,
		JpaSpecificationExecutor<FeedItem>,
		FeedItemRepositoryCustom {

	Optional<FeedItem> findByPostIdAndDeletedFalse(String postId);

	Optional<FeedItem> findByIdAndDeletedFalse(String id);
}
