package backend.simulation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "simulation_agent",
	uniqueConstraints = @UniqueConstraint(columnNames = {"runId", "agentId"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SimulationAgent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String runId;

	@Column(nullable = false)
	private String agentId;

	@Column(nullable = false)
	private String agentRole;

	@Column(nullable = false)
	private String archetype;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String emoji;

	@Column(nullable = false)
	private String homeRegionId;

	@Column(nullable = false)
	private String category;

	@Column(nullable = false)
	private String intent;
}
