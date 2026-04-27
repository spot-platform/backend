package backend.spot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.spot.entity.SpotChecklist;

public interface SpotChecklistRepository extends JpaRepository<SpotChecklist, Long> {

	List<SpotChecklist> findBySpotId(String spotId);

	List<SpotChecklist> findBySpotIdAndIsDone(String spotId, Boolean isDone);
}
