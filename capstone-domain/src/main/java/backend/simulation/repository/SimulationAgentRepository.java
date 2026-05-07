package backend.simulation.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import backend.simulation.entity.SimulationAgent;

public interface SimulationAgentRepository extends JpaRepository<SimulationAgent, Long> {

	List<SimulationAgent> findByRunId(String runId);

	boolean existsByRunId(String runId);
}
