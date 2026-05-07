package backend.simulation.initializer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import backend.simulation.entity.SimulationAgent;
import backend.simulation.entity.SimulationLifecycleEvent;
import backend.simulation.entity.SimulationMovement;
import backend.simulation.entity.SimulationPlace;
import backend.simulation.entity.SimulationRun;
import backend.simulation.repository.SimulationAgentRepository;
import backend.simulation.repository.SimulationLifecycleEventRepository;
import backend.simulation.repository.SimulationMovementRepository;
import backend.simulation.repository.SimulationPlaceRepository;
import backend.simulation.repository.SimulationRunRepository;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SimulationDataInitializer implements CommandLineRunner {

	private static final String RUN_ID = "demo_run_001";
	private static final String BASE_PATH = "simulation/";

	private final SimulationRunRepository runRepository;
	private final SimulationAgentRepository agentRepository;
	private final SimulationPlaceRepository placeRepository;
	private final SimulationMovementRepository movementRepository;
	private final SimulationLifecycleEventRepository lifecycleEventRepository;
	private final ObjectMapper objectMapper;

	@Override
	@Transactional(rollbackFor = IOException.class)
	public void run(String... args) throws IOException {
		if (agentRepository.existsByRunId(RUN_ID)) {
			return;
		}
		JsonNode manifest = readJson(BASE_PATH + "demo_run_001.manifest.json");
		seedRun(manifest);
		seedAgents(manifest, RUN_ID);
		seedPlaces(manifest, RUN_ID);
		seedMovements(RUN_ID);
		seedLifecycleEvents(RUN_ID);
	}

	private JsonNode readJson(String path) throws IOException {
		try (InputStream is = new ClassPathResource(path).getInputStream()) {
			return objectMapper.readTree(is);
		}
	}

	private void seedRun(JsonNode manifest) {
		runRepository.save(SimulationRun.builder()
			.runId(manifest.get("run_id").asText())
			.datasetVersion(manifest.get("dataset_version").asText())
			.approvedSpotCount(manifest.get("approved_spot_count").asInt())
			.filterKind(manifest.get("filter_kind").asText())
			.totalTicks(manifest.get("total_ticks").asInt())
			.tickDurationMsDefault(manifest.get("tick_duration_ms_default").asInt())
			.chunkSizeTicks(manifest.get("chunk_size_ticks").asInt())
			.build());
	}

	private void seedAgents(JsonNode manifest, String runId) {
		List<SimulationAgent> agents = new ArrayList<>();
		for (JsonNode node : manifest.get("agents")) {
			agents.add(SimulationAgent.builder()
				.runId(runId)
				.agentId(node.get("agent_id").asText())
				.agentRole(node.get("agent_role").asText())
				.archetype(node.get("archetype").asText())
				.name(node.get("name").asText())
				.emoji(node.get("emoji").asText())
				.homeRegionId(node.get("home_region_id").asText())
				.category(node.get("category").asText())
				.intent(node.get("intent").asText())
				.build());
		}
		agentRepository.saveAll(agents);
	}

	private void seedPlaces(JsonNode manifest, String runId) {
		List<SimulationPlace> places = new ArrayList<>();
		for (JsonNode node : manifest.get("places")) {
			places.add(SimulationPlace.builder()
				.runId(runId)
				.placeId(node.get("place_id").asText())
				.placeType(node.get("place_type").asText())
				.lat(node.get("lat").asDouble())
				.lng(node.get("lng").asDouble())
				.regionId(textOrNull(node, "region_id"))
				.label(textOrNull(node, "label"))
				.category(textOrNull(node, "category"))
				.intent(textOrNull(node, "intent"))
				.title(textOrNull(node, "title"))
				.build());
		}
		placeRepository.saveAll(places);
	}

	private void seedMovements(String runId) throws IOException {
		JsonNode root = readJson(BASE_PATH + "demo_run_001.movements.json");
		List<SimulationMovement> movements = new ArrayList<>();
		for (JsonNode node : root) {
			movements.add(SimulationMovement.builder()
				.runId(runId)
				.agentId(node.get("agent_id").asText())
				.departTick(node.get("depart_tick").asInt())
				.arriveTick(node.get("arrive_tick").asInt())
				.fromPlaceId(node.get("from_place_id").asText())
				.toPlaceId(node.get("to_place_id").asText())
				.reason(node.get("reason").asText())
				.spotId(textOrNull(node, "spot_id"))
				.build());
		}
		movementRepository.saveAll(movements);
	}

	private void seedLifecycleEvents(String runId) throws IOException {
		JsonNode root = readJson(BASE_PATH + "demo_run_001.lifecycle.json");
		List<SimulationLifecycleEvent> events = new ArrayList<>();
		for (JsonNode node : root) {
			events.add(SimulationLifecycleEvent.builder()
				.runId(runId)
				.tick(node.get("tick").asInt())
				.eventType(node.get("event_type").asText())
				.spotId(node.get("spot_id").asText())
				.agentId(textOrNull(node, "agent_id"))
				.build());
		}
		lifecycleEventRepository.saveAll(events);
	}

	private String textOrNull(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null || value.isNull()) {
			return null;
		}
		return value.asText();
	}
}
