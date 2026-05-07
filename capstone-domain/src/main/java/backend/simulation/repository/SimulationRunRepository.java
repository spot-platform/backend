package backend.simulation.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.simulation.entity.SimulationRun;

public interface SimulationRunRepository extends JpaRepository<SimulationRun, String> {
}
