package backend.feed.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import backend.feed.entity.Bookmark;

@Repository
public interface BookmarkRepository extends JpaRepository<Bookmark, String> {

	Optional<Bookmark> findByUserIdAndFeedItemId(String userId, String feedItemId);

	boolean existsByUserIdAndFeedItemId(String userId, String feedItemId);
}
