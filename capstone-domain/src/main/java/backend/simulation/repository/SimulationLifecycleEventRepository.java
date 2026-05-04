package backend.simulation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.simulation.entity.SimulationLifecycleEvent;

public interface SimulationLifecycleEventRepository extends JpaRepository<SimulationLifecycleEvent, Long> {

	List<SimulationLifecycleEvent> findByRunIdAndTickGreaterThanEqualAndTickLessThanOrderByTickAsc(
		String runId, int fromTick, int toTick
	);
}
