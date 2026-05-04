package backend.simulation.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SimManifestResponse {

	@JsonProperty("run_id")
	private String runId;

	@JsonProperty("dataset_version")
	private String datasetVersion;

	@JsonProperty("approved_spot_count")
	private int approvedSpotCount;

	@JsonProperty("filter_kind")
	private String filterKind;

	@JsonProperty("total_ticks")
	private int totalTicks;

	@JsonProperty("tick_duration_ms_default")
	private int tickDurationMsDefault;

	@JsonProperty("chunk_size_ticks")
	private int chunkSizeTicks;

	private List<SimAgentDto> agents;

	private List<SimPlaceDto> places;

	@Getter
	@Builder
	public static class SimAgentDto {

		@JsonProperty("agent_id")
		private String agentId;

		@JsonProperty("agent_role")
		private String agentRole;

		private String archetype;

		private String name;

		private String emoji;

		@JsonProperty("home_region_id")
		private String homeRegionId;

		private String category;

		private String intent;
	}

	@Getter
	@Builder
	public static class SimPlaceDto {

		@JsonProperty("place_id")
		private String placeId;

		@JsonProperty("place_type")
		private String placeType;

		private double lat;

		private double lng;

		@JsonProperty("region_id")
		private String regionId;

		private String label;

		private String category;

		private String intent;

		private String title;
	}
}
