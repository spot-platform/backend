package backend.simulation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.simulation.entity.SimulationPlace;

public interface SimulationPlaceRepository extends JpaRepository<SimulationPlace, Long> {

	List<SimulationPlace> findByRunId(String runId);
}
