package backend.spot.controller;

import java.util.List;

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

import backend.global.common.response.ApiResponse;
import backend.global.security.CustomUserDetails;
import backend.spot.dto.CastVoteRequest;
import backend.spot.dto.CreateChecklistRequest;
import backend.spot.dto.CreateNoteRequest;
import backend.spot.dto.CreateScheduleRequest;
import backend.spot.dto.CreateSpotRequest;
import backend.spot.dto.CreateVoteRequest;
import backend.spot.dto.SpotChecklistResponse;
import backend.spot.dto.SpotFileResponse;
import backend.spot.dto.SpotListResponse;
import backend.spot.dto.SpotNoteResponse;
import backend.spot.dto.SpotParticipantResponse;
import backend.spot.dto.SpotResponse;
import backend.spot.dto.SpotScheduleResponse;
import backend.spot.dto.SpotVoteResponse;
import backend.spot.dto.SubmitVoteAnswersRequest;
import backend.spot.dto.UploadFileRequest;
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

	// ─── Spot 기본 CRUD ───────────────────────────

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
	public ResponseEntity<ApiResponse<SpotResponse>> createSpot(
		@Valid @RequestBody CreateSpotRequest request,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(
			spotService.createSpot(request, resolveCurrentUserId(userDetails))
		));
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

	// ─── 참여자 (Participant) ─────────────────────

	@Operation(summary = "스팟 참여자 조회")
	@GetMapping("/{spotId}/participants")
	public ResponseEntity<ApiResponse<List<SpotParticipantResponse>>> getParticipants(
		@PathVariable String spotId
	) {
		return ResponseEntity.ok(ApiResponse.success(spotService.getParticipants(spotId)));
	}

	// ─── 일정 (Schedule) ─────────────────────────

	@Operation(summary = "스팟 일정 목록 조회")
	@GetMapping("/{spotId}/schedule")
	public ResponseEntity<ApiResponse<List<SpotScheduleResponse>>> getSchedule(@PathVariable String spotId) {
		return ResponseEntity.ok(ApiResponse.success(spotService.getSchedules(spotId)));
	}

	@Operation(summary = "스팟 일정 추가")
	@PutMapping("/{spotId}/schedule")
	public ResponseEntity<ApiResponse<SpotScheduleResponse>> updateSchedule(
		@PathVariable String spotId,
		@Valid @RequestBody CreateScheduleRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success(spotService.addSchedule(spotId, request)));
	}

	// ─── 투표 (Vote) ──────────────────────────────

	@Operation(summary = "스팟 투표 목록 조회")
	@GetMapping("/{spotId}/votes")
	public ResponseEntity<ApiResponse<List<SpotVoteResponse>>> getVotes(
		@PathVariable String spotId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(
			spotService.getVotes(spotId, resolveCurrentUserId(userDetails))
		));
	}

	@Operation(summary = "스팟 투표 생성")
	@PostMapping("/{spotId}/votes")
	public ResponseEntity<ApiResponse<SpotVoteResponse>> createVote(
		@PathVariable String spotId,
		@Valid @RequestBody CreateVoteRequest request,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(
			spotService.createVote(spotId, request, resolveCurrentUserId(userDetails))
		));
	}

	@Operation(
		summary = "스팟 투표 참여 (토글, 단발 클릭형)",
		description = "이미 투표한 옵션을 다시 cast 하면 해제됩니다. 단일선택에서 다른 옵션을 cast 하면 표 변경. "
			+ "각 클릭이 즉시 반영되는 UX 에 적합 (SSE 와 잘 어울림). "
			+ "다중선택에서 '선택중 → 투표 버튼' UX 가 필요하면 PUT /my-answers 를 사용하세요."
	)
	@PostMapping("/{spotId}/votes/{voteId}/cast")
	public ResponseEntity<ApiResponse<SpotVoteResponse>> castVote(
		@PathVariable String spotId,
		@PathVariable Long voteId,
		@Valid @RequestBody CastVoteRequest request,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(
			spotService.castVote(spotId, voteId, request, resolveCurrentUserId(userDetails))
		));
	}

	@Operation(
		summary = "스팟 투표 답변 배치 제출 (카카오톡 다중선택 UX)",
		description = "body 의 optionIds 가 최종 확정 상태이며 서버가 diff 하여 추가/삭제를 한 트랜잭션에서 적용. "
			+ "빈 배열 [] = 전체 취소. 같은 body 두 번 = 변화 없음 (멱등). "
			+ "단일선택 투표는 0~1 개만 허용."
	)
	@PutMapping("/{spotId}/votes/{voteId}/my-answers")
	public ResponseEntity<ApiResponse<SpotVoteResponse>> submitAnswers(
		@PathVariable String spotId,
		@PathVariable Long voteId,
		@Valid @RequestBody SubmitVoteAnswersRequest request,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(
			spotService.submitAnswers(spotId, voteId, request, resolveCurrentUserId(userDetails))
		));
	}

	// ─── 체크리스트 (Checklist) ───────────────────

	@Operation(summary = "스팟 체크리스트 조회")
	@GetMapping("/{spotId}/checklist")
	public ResponseEntity<ApiResponse<List<SpotChecklistResponse>>> getChecklist(@PathVariable String spotId) {
		return ResponseEntity.ok(ApiResponse.success(spotService.getChecklist(spotId)));
	}

	@Operation(summary = "체크리스트 항목 추가")
	@PutMapping("/{spotId}/checklist")
	public ResponseEntity<ApiResponse<SpotChecklistResponse>> updateChecklist(
		@PathVariable String spotId,
		@Valid @RequestBody CreateChecklistRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success(spotService.addChecklistItem(spotId, request)));
	}

	@Operation(summary = "체크리스트 항목 완료 토글")
	@PatchMapping("/{spotId}/checklist/{itemId}/toggle")
	public ResponseEntity<ApiResponse<SpotChecklistResponse>> toggleChecklistItem(
		@PathVariable String spotId,
		@PathVariable Long itemId
	) {
		return ResponseEntity.ok(ApiResponse.success(spotService.toggleChecklistItem(spotId, itemId)));
	}

	// ─── 파일 (File) ──────────────────────────────

	@Operation(summary = "스팟 파일 목록 조회")
	@GetMapping("/{spotId}/files")
	public ResponseEntity<ApiResponse<List<SpotFileResponse>>> getFiles(@PathVariable String spotId) {
		return ResponseEntity.ok(ApiResponse.success(spotService.getFiles(spotId)));
	}

	@Operation(summary = "스팟 파일 등록")
	@PostMapping("/{spotId}/files")
	public ResponseEntity<ApiResponse<SpotFileResponse>> uploadFile(
		@PathVariable String spotId,
		@Valid @RequestBody UploadFileRequest request,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(
			spotService.uploadFile(spotId, request, resolveCurrentUserId(userDetails))
		));
	}

	@Operation(summary = "스팟 파일 삭제")
	@DeleteMapping("/{spotId}/files/{fileId}")
	public ResponseEntity<ApiResponse<Void>> deleteFile(
		@PathVariable String spotId,
		@PathVariable Long fileId
	) {
		spotService.deleteFile(spotId, fileId);
		return ResponseEntity.ok(ApiResponse.success());
	}

	// ─── 노트 (Note) ──────────────────────────────

	@Operation(summary = "스팟 노트 목록 조회")
	@GetMapping("/{spotId}/notes")
	public ResponseEntity<ApiResponse<List<SpotNoteResponse>>> getNotes(@PathVariable String spotId) {
		return ResponseEntity.ok(ApiResponse.success(spotService.getNotes(spotId)));
	}

	@Operation(summary = "스팟 노트 작성")
	@PostMapping("/{spotId}/notes")
	public ResponseEntity<ApiResponse<SpotNoteResponse>> createNote(
		@PathVariable String spotId,
		@Valid @RequestBody CreateNoteRequest request,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(
			spotService.createNote(spotId, request, resolveCurrentUserId(userDetails))
		));
	}

	/**
	 * @AuthenticationPrincipal 으로 받은 CustomUserDetails 에서 userId 를 꺼낸다.
	 * 비인증 (Anonymous 토큰) 의 경우 Spring 이 null 을 주입하므로 null 안전.
	 */
	private String resolveCurrentUserId(CustomUserDetails userDetails) {
		return userDetails == null ? null : userDetails.getUserId();
	}

	// ─── 리뷰 (Review) - TODO ─────────────────────

	@Operation(summary = "스팟 리뷰 조회", description = "TODO: Review 도메인 구현 후 연결")
	@GetMapping("/{spotId}/reviews")
	public ResponseEntity<ApiResponse<Void>> getReviews(@PathVariable String spotId) {
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "스팟 리뷰 작성", description = "TODO: Review 도메인 구현 후 연결")
	@PostMapping("/{spotId}/reviews")
	public ResponseEntity<ApiResponse<Void>> createReview(@PathVariable String spotId) {
		return ResponseEntity.ok(ApiResponse.success());
	}
}
