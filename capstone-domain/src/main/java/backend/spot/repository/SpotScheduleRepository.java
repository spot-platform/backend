package backend.spot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.spot.entity.SpotSchedule;

public interface SpotScheduleRepository extends JpaRepository<SpotSchedule, Long> {

	List<SpotSchedule> findBySpotId(String spotId);

	List<SpotSchedule> findBySpotIdOrderByScheduledAtAsc(String spotId);
}
