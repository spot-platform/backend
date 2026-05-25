package backend.spot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.spot.entity.SpotReview;

public interface SpotReviewRepository extends JpaRepository<SpotReview, Long> {

	List<SpotReview> findBySpotIdOrderByCreatedAtDesc(Long spotId);

	boolean existsBySpotIdAndReviewerIdAndTargetNickname(Long spotId, String reviewerId, String targetNickname);
}
