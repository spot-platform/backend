package backend.simulation.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import backend.global.error.exception.BusinessException;
import backend.global.error.exception.ErrorCode;
import backend.simulation.dto.LifecycleChunkResponse;
import backend.simulation.dto.LifecycleChunkResponse.LifecycleEventDto;
import backend.simulation.dto.MovementChunkResponse;
import backend.simulation.dto.MovementChunkResponse.MovementDto;
import backend.simulation.dto.SimManifestResponse;
import backend.simulation.dto.SimManifestResponse.SimAgentDto;
import backend.simulation.dto.SimManifestResponse.SimPlaceDto;
import backend.simulation.entity.SimulationRun;
import backend.simulation.repository.SimulationAgentRepository;
import backend.simulation.repository.SimulationLifecycleEventRepository;
import backend.simulation.repository.SimulationMovementRepository;
import backend.simulation.repository.SimulationPlaceRepository;
import backend.simulation.repository.SimulationRunRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SimulationService {

	private final SimulationRunRepository runRepository;
	private final SimulationAgentRepository agentRepository;
	private final SimulationPlaceRepository placeRepository;
	private final SimulationMovementRepository movementRepository;
	private final SimulationLifecycleEventRepository lifecycleEventRepository;

	private static final int MAX_TICK_WINDOW = 1000;

	private void validateTickWindow(int fromTick, int toTick) {
		if (fromTick < 0 || toTick <= fromTick || (toTick - fromTick) > MAX_TICK_WINDOW) {
			throw new BusinessException(ErrorCode.INVALID_TICK_WINDOW);
		}
	}

	public SimManifestResponse getManifest(String runId) {
		SimulationRun run = runRepository.findById(runId)
			.orElseThrow(() -> new BusinessException(ErrorCode.SIMULATION_RUN_NOT_FOUND));

		List<SimAgentDto> agents = agentRepository.findByRunId(runId).stream()
			.map(a -> SimAgentDto.builder()
				.agentId(a.getAgentId())
				.agentRole(a.getAgentRole())
				.archetype(a.getArchetype())
				.name(a.getName())
				.emoji(a.getEmoji())
				.homeRegionId(a.getHomeRegionId())
				.category(a.getCategory())
				.intent(a.getIntent())
				.build())
			.toList();

		List<SimPlaceDto> places = placeRepository.findByRunId(runId).stream()
			.map(p -> SimPlaceDto.builder()
				.placeId(p.getPlaceId())
				.placeType(p.getPlaceType())
				.lat(p.getLat())
				.lng(p.getLng())
				.regionId(p.getRegionId())
				.label(p.getLabel())
				.category(p.getCategory())
				.intent(p.getIntent())
				.title(p.getTitle())
				.build())
			.toList();

		return SimManifestResponse.builder()
			.runId(run.getRunId())
			.datasetVersion(run.getDatasetVersion())
			.approvedSpotCount(run.getApprovedSpotCount())
			.filterKind(run.getFilterKind())
			.totalTicks(run.getTotalTicks())
			.tickDurationMsDefault(run.getTickDurationMsDefault())
			.chunkSizeTicks(run.getChunkSizeTicks())
			.agents(agents)
			.places(places)
			.build();
	}

	public MovementChunkResponse getMovements(String runId, int fromTick, int toTick) {
		validateTickWindow(fromTick, toTick);
		if (!runRepository.existsById(runId)) {
			throw new BusinessException(ErrorCode.SIMULATION_RUN_NOT_FOUND);
		}

		List<MovementDto> movements = movementRepository
			.findByRunIdAndDepartTickGreaterThanEqualAndDepartTickLessThanOrderByDepartTickAscIdAsc(runId, fromTick, toTick)
			.stream()
			.map(m -> MovementDto.builder()
				.agentId(m.getAgentId())
				.departTick(m.getDepartTick())
				.arriveTick(m.getArriveTick())
				.fromPlaceId(m.getFromPlaceId())
				.toPlaceId(m.getToPlaceId())
				.reason(m.getReason())
				.spotId(m.getSpotId())
				.build())
			.toList();

		return MovementChunkResponse.builder()
			.runId(runId)
			.fromTick(fromTick)
			.toTick(toTick)
			.movements(movements)
			.build();
	}

	public LifecycleChunkResponse getLifecycle(String runId, int fromTick, int toTick) {
		validateTickWindow(fromTick, toTick);
		if (!runRepository.existsById(runId)) {
			throw new BusinessException(ErrorCode.SIMULATION_RUN_NOT_FOUND);
		}

		List<LifecycleEventDto> events = lifecycleEventRepository
			.findByRunIdAndTickGreaterThanEqualAndTickLessThanOrderByTickAsc(runId, fromTick, toTick)
			.stream()
			.map(e -> LifecycleEventDto.builder()
				.tick(e.getTick())
				.eventType(e.getEventType())
				.spotId(e.getSpotId())
				.agentId(e.getAgentId())
				.build())
			.toList();

		return LifecycleChunkResponse.builder()
			.runId(runId)
			.fromTick(fromTick)
			.toTick(toTick)
			.events(events)
			.build();
	}
}
