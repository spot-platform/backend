package backend.simulation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import backend.global.common.response.ApiResponse;
import backend.simulation.dto.LifecycleChunkResponse;
import backend.simulation.dto.MovementChunkResponse;
import backend.simulation.dto.SimManifestResponse;
import backend.simulation.service.SimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Simulation API", description = "맵 시뮬레이션 재생 API")
@RestController
@RequestMapping("/api/sim/runs")
@RequiredArgsConstructor
public class SimulationController {

	private final SimulationService simulationService;

	@Operation(summary = "시뮬레이션 런 매니페스트 조회")
	@GetMapping("/{runId}/manifest")
	public ResponseEntity<ApiResponse<SimManifestResponse>> getManifest(
		@PathVariable String runId
	) {
		return ResponseEntity.ok(ApiResponse.success(simulationService.getManifest(runId)));
	}

	@Operation(summary = "이동 데이터 청크 조회")
	@GetMapping("/{runId}/movements")
	public ResponseEntity<ApiResponse<MovementChunkResponse>> getMovements(
		@PathVariable String runId,
		@RequestParam int fromTick,
		@RequestParam int toTick
	) {
		return ResponseEntity.ok(ApiResponse.success(simulationService.getMovements(runId, fromTick, toTick)));
	}

	@Operation(summary = "라이프사이클 이벤트 청크 조회")
	@GetMapping("/{runId}/lifecycle")
	public ResponseEntity<ApiResponse<LifecycleChunkResponse>> getLifecycle(
		@PathVariable String runId,
		@RequestParam int fromTick,
		@RequestParam int toTick
	) {
		return ResponseEntity.ok(ApiResponse.success(simulationService.getLifecycle(runId, fromTick, toTick)));
	}
}
