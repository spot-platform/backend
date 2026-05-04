package backend.simulation.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LifecycleChunkResponse {

	@JsonProperty("run_id")
	private String runId;

	@JsonProperty("from_tick")
	private int fromTick;

	@JsonProperty("to_tick")
	private int toTick;

	private List<LifecycleEventDto> events;

	@Getter
	@Builder
	public static class LifecycleEventDto {

		private int tick;

		@JsonProperty("event_type")
		private String eventType;

		@JsonProperty("spot_id")
		private String spotId;

		@JsonProperty("agent_id")
		private String agentId;
	}
}
