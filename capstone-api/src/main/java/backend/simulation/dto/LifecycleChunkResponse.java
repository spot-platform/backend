package backend.simulation.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

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

		private JsonNode payload;

		@JsonProperty("scheduled_tick")
		private Integer scheduledTick;

		@JsonProperty("schedule_lead_ticks")
		private Integer scheduleLeadTicks;

		@JsonProperty("duration_ticks")
		private Integer durationTicks;

		@JsonProperty("expected_closed_at_tick")
		private Integer expectedClosedAtTick;

		@JsonProperty("map_anchor")
		private JsonNode mapAnchor;

		@JsonProperty("hotspot_signal")
		private JsonNode hotspotSignal;
	}
}
