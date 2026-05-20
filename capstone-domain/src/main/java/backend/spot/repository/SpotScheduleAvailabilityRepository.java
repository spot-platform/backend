package backend.spot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.spot.entity.SpotScheduleAvailability;

public interface SpotScheduleAvailabilityRepository extends JpaRepository<SpotScheduleAvailability, String> {

	List<SpotScheduleAvailability> findBySlotIdIn(List<Long> slotIds);

	void deleteBySlotIdIn(List<Long> slotIds);
}
