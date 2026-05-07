package backend.simulation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "simulation_run")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SimulationRun {

	@Id
	@Column(name = "run_id")
	private String runId;

	@Column(nullable = false)
	private String datasetVersion;

	@Column(nullable = false)
	private int approvedSpotCount;

	@Column(nullable = false)
	private String filterKind;

	@Column(nullable = false)
	private int totalTicks;

	@Column(nullable = false)
	private int tickDurationMsDefault;

	@Column(nullable = false)
	private int chunkSizeTicks;
}
