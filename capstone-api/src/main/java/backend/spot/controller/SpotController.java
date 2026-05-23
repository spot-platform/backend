package backend.spot.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import backend.global.common.response.ApiResponse;
import backend.global.security.CustomUserDetails;
import backend.spot.dto.AssignChecklistRequest;
import backend.spot.dto.CreateChecklistRequest;
import backend.spot.dto.CreateNoteRequest;
import backend.spot.dto.CreateSpotRequest;
import backend.spot.dto.SpotChecklistResponse;
import backend.spot.dto.SpotFileResponse;
import backend.spot.dto.SpotListResponse;
import backend.spot.dto.SpotMapItemResponse;
import backend.spot.dto.SpotNoteResponse;
import backend.spot.dto.SpotParticipantResponse;
import backend.spot.dto.SpotResponse;
import backend.spot.dto.SpotScheduleResponse;
import backend.spot.dto.UpdateScheduleRequest;
import backend.spot.dto.UploadFileRequest;
import backend.spot.service.SpotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Spot API", description = "스팟 관리 API")
@RestController
@RequestMapping("/api/v1/spots")
@RequiredArgsConstructor
public class SpotController {

	private final SpotService spotService;

	// ─── Spot 기본 CRUD ───────────────────────────

	@Operation(summary = "스팟 목록 조회")
	@GetMapping
	public ResponseEntity<ApiResponse<SpotListResponse>> getSpots(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(spotService.getSpots(page, size, currentUserIdOrNull(userDetails))));
	}

	@Operation(summary = "지도 마커용 스팟 목록", description = "bounds(sw/ne) 4개 모두 주거나 모두 생략. type/status/category 선택 필터.")
	@GetMapping("/map")
	public ResponseEntity<ApiResponse<List<SpotMapItemResponse>>> getSpotMap(
		@RequestParam(required = false) Double swLat,
		@RequestParam(required = false) Double swLng,
		@RequestParam(required = false) Double neLat,
		@RequestParam(required = false) Double neLng,
		@RequestParam(required = false) String category,
		@RequestParam(required = false) String type,
		@RequestParam(required = false) String status
	) {
		return ResponseEntity.ok(ApiResponse.success(
			spotService.getSpotMap(swLat, swLng, neLat, neLng, category, type, status)
		));
	}

	@Operation(summary = "스팟 검색", description = "키워드 부분 일치 검색. scope=ALL(제목+설명, 기본) | TITLE(제목만) | CONTENT(설명만)")
	@GetMapping("/search")
	public ResponseEntity<ApiResponse<SpotListResponse>> searchSpots(
		@RequestParam String q,
		@RequestParam(defaultValue = "ALL") String scope,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "10") int size,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		SpotListResponse result = spotService.searchSpots(q, scope, page, size, currentUserIdOrNull(userDetails));
		return ResponseEntity.ok(ApiResponse.success(result));
	}

	@Operation(summary = "스팟 생성")
	@PostMapping
	public ResponseEntity<ApiResponse<SpotResponse>> createSpot(
		@Valid @RequestBody CreateSpotRequest request,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(
			spotService.createSpot(request, requireAuth(userDetails))
		));
	}

	@Operation(summary = "스팟 상세 조회")
	@GetMapping("/{spotId}")
	public ResponseEntity<ApiResponse<SpotResponse>> getSpot(
		@PathVariable Long spotId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(spotService.getSpot(spotId, currentUserIdOrNull(userDetails))));
	}

	@Operation(summary = "스팟 매칭", description = "스팟 상태를 OPEN → MATCHED로 전환합니다.")
	@PostMapping("/{spotId}/match")
	public ResponseEntity<ApiResponse<SpotResponse>> matchSpot(@PathVariable Long spotId) {
		return ResponseEntity.ok(ApiResponse.success(spotService.matchSpot(spotId)));
	}

	@Operation(summary = "스팟 취소", description = "스팟 상태를 OPEN → CLOSED로 전환합니다.")
	@PostMapping("/{spotId}/cancel")
	public ResponseEntity<ApiResponse<SpotResponse>> cancelSpot(@PathVariable Long spotId) {
		return ResponseEntity.ok(ApiResponse.success(spotService.cancelSpot(spotId)));
	}

	@Operation(summary = "스팟 완료", description = "스팟 상태를 MATCHED → CLOSED로 전환합니다.")
	@PostMapping("/{spotId}/complete")
	public ResponseEntity<ApiResponse<SpotResponse>> completeSpot(@PathVariable Long spotId) {
		return ResponseEntity.ok(ApiResponse.success(spotService.completeSpot(spotId)));
	}

	// ─── 참여자 (Participant) ─────────────────────

	@Operation(summary = "스팟 참여자 조회")
	@GetMapping("/{spotId}/participants")
	public ResponseEntity<ApiResponse<List<SpotParticipantResponse>>> getParticipants(
		@PathVariable Long spotId
	) {
		return ResponseEntity.ok(ApiResponse.success(spotService.getParticipants(spotId)));
	}

	// ─── 일정 (Schedule) ─────────────────────────

	@Operation(summary = "스팟 일정 조회", description = "제안 슬롯 목록 + 확정 슬롯")
	@GetMapping("/{spotId}/schedule")
	public ResponseEntity<ApiResponse<SpotScheduleResponse>> getSchedule(@PathVariable Long spotId) {
		return ResponseEntity.ok(ApiResponse.success(spotService.getSchedule(spotId)));
	}

	@Operation(summary = "스팟 일정 저장", description = "proposedSlots 전체 교체 + confirmedSlot 확정 (참여자만)")
	@PutMapping("/{spotId}/schedule")
	public ResponseEntity<ApiResponse<SpotScheduleResponse>> updateSchedule(
		@PathVariable Long spotId,
		@Valid @RequestBody UpdateScheduleRequest request,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(
			spotService.updateSchedule(spotId, request, requireAuth(userDetails))
		));
	}

	// ─── 체크리스트 (Checklist) ───────────────────

	@Operation(summary = "스팟 체크리스트 조회")
	@GetMapping("/{spotId}/checklist")
	public ResponseEntity<ApiResponse<List<SpotChecklistResponse>>> getChecklist(@PathVariable Long spotId) {
		return ResponseEntity.ok(ApiResponse.success(spotService.getChecklist(spotId)));
	}

	@Operation(summary = "체크리스트 항목 추가")
	@PutMapping("/{spotId}/checklist")
	public ResponseEntity<ApiResponse<SpotChecklistResponse>> updateChecklist(
		@PathVariable Long spotId,
		@Valid @RequestBody CreateChecklistRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success(spotService.addChecklistItem(spotId, request)));
	}

	@Operation(summary = "체크리스트 항목 완료 토글")
	@PatchMapping("/{spotId}/checklist/{itemId}/toggle")
	public ResponseEntity<ApiResponse<SpotChecklistResponse>> toggleChecklistItem(
		@PathVariable Long spotId,
		@PathVariable Long itemId
	) {
		return ResponseEntity.ok(ApiResponse.success(spotService.toggleChecklistItem(spotId, itemId)));
	}

	@Operation(summary = "체크리스트 항목 담당자 지정/해제", description = "assigneeId=null 이면 담당자 해제. 참여자만 가능.")
	@PatchMapping("/{spotId}/checklist/{itemId}/assignee")
	public ResponseEntity<ApiResponse<SpotChecklistResponse>> assignChecklistItem(
		@PathVariable Long spotId,
		@PathVariable Long itemId,
		@RequestBody AssignChecklistRequest request,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(
			spotService.assignChecklistItem(spotId, itemId, request.getAssigneeId(), requireAuth(userDetails))
		));
	}

	// ─── 파일 (File) ──────────────────────────────

	@Operation(summary = "스팟 파일 목록 조회")
	@GetMapping("/{spotId}/files")
	public ResponseEntity<ApiResponse<List<SpotFileResponse>>> getFiles(@PathVariable Long spotId) {
		return ResponseEntity.ok(ApiResponse.success(spotService.getFiles(spotId)));
	}

	@Operation(summary = "스팟 파일 등록")
	@PostMapping("/{spotId}/files")
	public ResponseEntity<ApiResponse<SpotFileResponse>> uploadFile(
		@PathVariable Long spotId,
		@Valid @RequestBody UploadFileRequest request,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(
			spotService.uploadFile(spotId, request, requireAuth(userDetails))
		));
	}

	@Operation(summary = "스팟 파일 삭제")
	@DeleteMapping("/{spotId}/files/{fileId}")
	public ResponseEntity<ApiResponse<Void>> deleteFile(
		@PathVariable Long spotId,
		@PathVariable Long fileId
	) {
		spotService.deleteFile(spotId, fileId);
		return ResponseEntity.ok(ApiResponse.success());
	}

	// ─── 노트 (Note) ──────────────────────────────

	@Operation(summary = "스팟 노트 목록 조회")
	@GetMapping("/{spotId}/notes")
	public ResponseEntity<ApiResponse<List<SpotNoteResponse>>> getNotes(@PathVariable Long spotId) {
		return ResponseEntity.ok(ApiResponse.success(spotService.getNotes(spotId)));
	}

	@Operation(summary = "스팟 노트 작성")
	@PostMapping("/{spotId}/notes")
	public ResponseEntity<ApiResponse<SpotNoteResponse>> createNote(
		@PathVariable Long spotId,
		@Valid @RequestBody CreateNoteRequest request,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(
			spotService.createNote(spotId, request, requireAuth(userDetails))
		));
	}

	private String requireAuth(CustomUserDetails userDetails) {
		if (userDetails == null || userDetails.getUserId() == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
		}
		return userDetails.getUserId();
	}

	// 공개 조회용 — 비인증이면 null (isOwner 계산 등에 사용)
	private String currentUserIdOrNull(CustomUserDetails userDetails) {
		return userDetails != null ? userDetails.getUserId() : null;
	}

	// ─── 리뷰 (Review) - TODO ─────────────────────

	@Operation(summary = "스팟 리뷰 조회", description = "TODO: Review 도메인 구현 후 연결")
	@GetMapping("/{spotId}/reviews")
	public ResponseEntity<ApiResponse<Void>> getReviews(@PathVariable Long spotId) {
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "스팟 리뷰 작성", description = "TODO: Review 도메인 구현 후 연결")
	@PostMapping("/{spotId}/reviews")
	public ResponseEntity<ApiResponse<Void>> createReview(@PathVariable Long spotId) {
		return ResponseEntity.ok(ApiResponse.success());
	}
}
