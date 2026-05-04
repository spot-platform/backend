package backend.simulation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.simulation.entity.SimulationMovement;

public interface SimulationMovementRepository extends JpaRepository<SimulationMovement, Long> {

	List<SimulationMovement> findByRunIdAndDepartTickGreaterThanEqualAndDepartTickLessThan(
		String runId, int fromTick, int toTick
	);
}
