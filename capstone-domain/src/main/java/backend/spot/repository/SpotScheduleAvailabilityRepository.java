package backend.spot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import backend.spot.entity.SpotScheduleAvailability;

public interface SpotScheduleAvailabilityRepository extends JpaRepository<SpotScheduleAvailability, String> {

	List<SpotScheduleAvailability> findBySlotIdIn(List<Long> slotIds);

	@Modifying
	@Query("DELETE FROM SpotScheduleAvailability a WHERE a.slotId IN :slotIds")
	void deleteBySlotIdIn(@Param("slotIds") List<Long> slotIds);
}
