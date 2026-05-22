package backend.feed.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import backend.feed.entity.Bookmark;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, String> {

	Optional<Bookmark> findByUserIdAndFeedItemId(String userId, Long feedItemId);

	boolean existsByUserIdAndFeedItemId(String userId, Long feedItemId);

	List<Bookmark> findByUserIdAndFeedItemIdIn(String userId, List<Long> feedItemIds);
}
