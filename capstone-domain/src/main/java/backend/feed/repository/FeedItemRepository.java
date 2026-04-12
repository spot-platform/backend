package backend.feed.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import backend.feed.entity.FeedItem;

@Repository
public interface FeedItemRepository extends JpaRepository<FeedItem, String>,
		JpaSpecificationExecutor<FeedItem>,
		FeedItemRepositoryCustom {
}
