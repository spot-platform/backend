package backend.spot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.spot.entity.SpotTimelineEvent;

public interface SpotTimelineEventRepository extends JpaRepository<SpotTimelineEvent, Long> {

	List<SpotTimelineEvent> findBySpotIdOrderByCreatedAtAscIdAsc(Long spotId);
}
