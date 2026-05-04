package backend.simulation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
	name = "simulation_movement",
	indexes = @Index(name = "idx_movement_runid_depart", columnList = "runId, departTick")
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SimulationMovement {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String runId;

	@Column(nullable = false)
	private String agentId;

	@Column(nullable = false)
	private int departTick;

	@Column(nullable = false)
	private int arriveTick;

	@Column(nullable = false)
	private String fromPlaceId;

	@Column(nullable = false)
	private String toPlaceId;

	@Column(nullable = false)
	private String reason;

	private String spotId;
}
