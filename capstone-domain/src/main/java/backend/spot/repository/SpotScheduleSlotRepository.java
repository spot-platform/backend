package backend.spot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import backend.spot.entity.SpotScheduleSlot;

public interface SpotScheduleSlotRepository extends JpaRepository<SpotScheduleSlot, Long> {

	List<SpotScheduleSlot> findBySpotIdOrderBySlotDateAscSlotHourAsc(Long spotId);

	@Modifying
	@Query("DELETE FROM SpotScheduleSlot s WHERE s.spotId = :spotId")
	void deleteBySpotId(@Param("spotId") Long spotId);
}
