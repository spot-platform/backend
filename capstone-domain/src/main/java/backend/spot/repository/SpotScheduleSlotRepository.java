package backend.spot.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.spot.entity.SpotScheduleSlot;

public interface SpotScheduleSlotRepository extends JpaRepository<SpotScheduleSlot, Long> {

	List<SpotScheduleSlot> findBySpotIdOrderBySlotDateAscSlotHourAsc(String spotId);

	void deleteBySpotId(String spotId);
}
