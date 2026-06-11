package backend.simulation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import backend.simulation.entity.SimulationMovement;

public interface SimulationMovementRepository extends JpaRepository<SimulationMovement, Long> {

	List<SimulationMovement> findByRunIdAndDepartTickGreaterThanEqualAndDepartTickLessThanOrderByDepartTickAscIdAsc(
		String runId, int fromTick, int toTick
	);

	@Query("select max(m.arriveTick) from SimulationMovement m where m.runId = :runId")
	Optional<Integer> findMaxArriveTickByRunId(@Param("runId") String runId);
}
