package backend.simulation.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MovementChunkResponse {

	@JsonProperty("run_id")
	private String runId;

	@JsonProperty("from_tick")
	private int fromTick;

	@JsonProperty("to_tick")
	private int toTick;

	private List<MovementDto> movements;

	@Getter
	@Builder
	public static class MovementDto {

		@JsonProperty("agent_id")
		private String agentId;

		@JsonProperty("depart_tick")
		private int departTick;

		@JsonProperty("arrive_tick")
		private int arriveTick;

		@JsonProperty("from_place_id")
		private String fromPlaceId;

		@JsonProperty("to_place_id")
		private String toPlaceId;

		private String reason;

		@JsonProperty("spot_id")
		private String spotId;
	}
}
