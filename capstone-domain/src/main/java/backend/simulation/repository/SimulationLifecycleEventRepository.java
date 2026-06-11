package backend.simulation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import backend.simulation.entity.SimulationLifecycleEvent;

public interface SimulationLifecycleEventRepository extends JpaRepository<SimulationLifecycleEvent, Long> {

	List<SimulationLifecycleEvent> findByRunIdAndTickGreaterThanEqualAndTickLessThanOrderByTickAsc(
		String runId, int fromTick, int toTick
	);

	@Query("select max(e.tick) from SimulationLifecycleEvent e where e.runId = :runId")
	Optional<Integer> findMaxTickByRunId(@Param("runId") String runId);
}
