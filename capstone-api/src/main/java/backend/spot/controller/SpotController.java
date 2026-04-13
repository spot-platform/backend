package backend.spot.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import backend.global.common.response.ApiResponse;
import backend.spot.dto.CreateSpotRequest;
import backend.spot.dto.SpotListResponse;
import backend.spot.dto.SpotResponse;
import backend.spot.service.SpotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Spot API", description = "스팟 관리 API")
@RestController
@RequestMapping("/api/spots")
@RequiredArgsConstructor
public class SpotController {

	private final SpotService spotService;

	@Operation(summary = "스팟 목록 조회")
	@GetMapping
	public ResponseEntity<ApiResponse<SpotListResponse>> getSpots(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size
	) {
		return ResponseEntity.ok(ApiResponse.success(spotService.getSpots(page, size)));
	}

	@Operation(summary = "스팟 생성")
	@PostMapping
	public ResponseEntity<ApiResponse<SpotResponse>> createSpot(@Valid @RequestBody CreateSpotRequest request) {
		return ResponseEntity.ok(ApiResponse.success(spotService.createSpot(request)));
	}

	@Operation(summary = "스팟 상세 조회")
	@GetMapping("/{spotId}")
	public ResponseEntity<ApiResponse<SpotResponse>> getSpot(@PathVariable String spotId) {
		return ResponseEntity.ok(ApiResponse.success(spotService.getSpot(spotId)));
	}

	@Operation(summary = "스팟 매칭", description = "스팟 상태를 OPEN → MATCHED로 전환합니다.")
	@PostMapping("/{spotId}/match")
	public ResponseEntity<ApiResponse<SpotResponse>> matchSpot(@PathVariable String spotId) {
		return ResponseEntity.ok(ApiResponse.success(spotService.matchSpot(spotId)));
	}

	@Operation(summary = "스팟 취소", description = "스팟 상태를 OPEN → CLOSED로 전환합니다.")
	@PostMapping("/{spotId}/cancel")
	public ResponseEntity<ApiResponse<SpotResponse>> cancelSpot(@PathVariable String spotId) {
		return ResponseEntity.ok(ApiResponse.success(spotService.cancelSpot(spotId)));
	}

	@Operation(summary = "스팟 완료", description = "스팟 상태를 MATCHED → CLOSED로 전환합니다.")
	@PostMapping("/{spotId}/complete")
	public ResponseEntity<ApiResponse<SpotResponse>> completeSpot(@PathVariable String spotId) {
		return ResponseEntity.ok(ApiResponse.success(spotService.completeSpot(spotId)));
	}

	@Operation(summary = "스팟 참여자 조회")
	@GetMapping("/{spotId}/participants")
	public ResponseEntity<ApiResponse<Void>> getParticipants(@PathVariable String spotId) {
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "스팟 일정 조회")
	@GetMapping("/{spotId}/schedule")
	public ResponseEntity<ApiResponse<Void>> getSchedule(@PathVariable String spotId) {
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "스팟 일정 수정")
	@PutMapping("/{spotId}/schedule")
	public ResponseEntity<ApiResponse<Void>> updateSchedule(@PathVariable String spotId) {
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "스팟 투표 조회")
	@GetMapping("/{spotId}/votes")
	public ResponseEntity<ApiResponse<Void>> getVotes(@PathVariable String spotId) {
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "스팟 투표 생성")
	@PostMapping("/{spotId}/votes")
	public ResponseEntity<ApiResponse<Void>> createVote(@PathVariable String spotId) {
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "스팟 투표 참여")
	@PostMapping("/{spotId}/votes/{voteId}/cast")
	public ResponseEntity<ApiResponse<Void>> castVote(@PathVariable String spotId, @PathVariable Long voteId) {
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "스팟 체크리스트 조회")
	@GetMapping("/{spotId}/checklist")
	public ResponseEntity<ApiResponse<Void>> getChecklist(@PathVariable String spotId) {
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "스팟 체크리스트 수정")
	@PutMapping("/{spotId}/checklist")
	public ResponseEntity<ApiResponse<Void>> updateChecklist(@PathVariable String spotId) {
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "스팟 파일 조회")
	@GetMapping("/{spotId}/files")
	public ResponseEntity<ApiResponse<Void>> getFiles(@PathVariable String spotId) {
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "스팟 파일 업로드")
	@PostMapping("/{spotId}/files")
	public ResponseEntity<ApiResponse<Void>> uploadFile(@PathVariable String spotId) {
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "스팟 파일 삭제")
	@DeleteMapping("/{spotId}/files/{fileId}")
	public ResponseEntity<ApiResponse<Void>> deleteFile(@PathVariable String spotId, @PathVariable Long fileId) {
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "스팟 노트 조회")
	@GetMapping("/{spotId}/notes")
	public ResponseEntity<ApiResponse<Void>> getNotes(@PathVariable String spotId) {
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "스팟 노트 생성")
	@PostMapping("/{spotId}/notes")
	public ResponseEntity<ApiResponse<Void>> createNote(@PathVariable String spotId) {
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "스팟 리뷰 조회")
	@GetMapping("/{spotId}/reviews")
	public ResponseEntity<ApiResponse<Void>> getReviews(@PathVariable String spotId) {
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "스팟 리뷰 작성")
	@PostMapping("/{spotId}/reviews")
	public ResponseEntity<ApiResponse<Void>> createReview(@PathVariable String spotId) {
		return ResponseEntity.ok(ApiResponse.success());
	}
}
