package backend.chat.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import backend.chat.dto.AddChatVoteOptionRequest;
import backend.chat.dto.CastChatVoteRequest;
import backend.chat.dto.ChatBlockResponse;
import backend.chat.dto.ChatMemberResponse;
import backend.chat.dto.ChatMessageListResponse;
import backend.chat.dto.ChatMessageResponse;
import backend.chat.dto.ChatNotificationSettingResponse;
import backend.chat.dto.ChatRoomResponse;
import backend.chat.dto.ChatVoteResponse;
import backend.chat.dto.CreateChatBlockRequest;
import backend.chat.dto.CreateChatRoomRequest;
import backend.chat.dto.CreateChatVoteRequest;
import backend.chat.dto.CreatePersonalChatRoomRequest;
import backend.chat.dto.SendMessageRequest;
import backend.chat.dto.SubmitChatVoteAnswersRequest;
import backend.chat.dto.UpdateChatNotificationRequest;
import backend.chat.entity.ChatRoomType;
import backend.chat.service.ChatService;
import backend.chat.service.ChatVoteService;
import backend.chat.service.SseEmitterService;
import backend.global.common.response.ApiResponse;
import backend.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Chat API", description = "채팅 API")
@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

	private final ChatService chatService;
	private final ChatVoteService chatVoteService;
	private final SseEmitterService sseEmitterService;

	// ─── SSE 연결 ─────────────────────────────────

	@Operation(
		summary = "SSE 구독 연결 (방)",
		description = "특정 채팅방에 SSE 실시간 연결을 맺습니다. 연결 후 해당 방에 새 메시지가 오면 즉시 수신됩니다."
	)
	@GetMapping(value = "/rooms/{roomId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter connect(
		@Parameter(description = "구독할 채팅방 ID", required = true)
		@PathVariable Long roomId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		chatService.assertMembershipPublic(roomId, currentUserId(userDetails));
		return sseEmitterService.subscribe(roomId);
	}

	@Operation(
		summary = "SSE 구독 연결 (유저)",
		description = "채팅 목록 화면의 unread 배지를 실시간으로 갱신하기 위한 유저 레벨 SSE 연결입니다. "
			+ "다른 방에 새 메시지가 오면 badge_update 이벤트가 도착합니다."
	)
	@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter connectUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
		if (userDetails == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
		}
		return sseEmitterService.subscribeUser(currentUserId(userDetails));
	}

	// ─── 채팅방 (Room) ─────────────────────────────

	@Operation(summary = "채팅방 목록 조회")
	@GetMapping("/rooms")
	public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getRooms(
		@RequestParam(required = false) ChatRoomType type,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(chatService.getRooms(currentUserId(userDetails), type)));
	}

	@Operation(
		summary = "그룹 채팅방 생성",
		description = "GROUP 타입만 허용. 동일 spotId 의 활성 GROUP 방이 있으면 idempotent 하게 기존 방을 반환합니다. "
			+ "PERSONAL 채팅은 POST /rooms/personal 을 사용하세요."
	)
	@PostMapping("/rooms")
	public ResponseEntity<ApiResponse<ChatRoomResponse>> createRoom(
		@Valid @RequestBody CreateChatRoomRequest request,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(
			chatService.createRoom(request, currentUserId(userDetails))
		));
	}

	@Operation(
		summary = "1:1 채팅방 시작",
		description = "현재 유저와 partnerId 사이의 PERSONAL 채팅방을 만들거나 기존 방을 반환합니다 (카카오톡 스타일)."
	)
	@PostMapping("/rooms/personal")
	public ResponseEntity<ApiResponse<ChatRoomResponse>> createPersonalRoom(
		@Valid @RequestBody CreatePersonalChatRoomRequest request,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(
			chatService.createPersonalRoom(request, currentUserId(userDetails))
		));
	}

	@Operation(summary = "채팅방 상세 조회")
	@GetMapping("/rooms/{roomId}")
	public ResponseEntity<ApiResponse<ChatRoomResponse>> getRoom(
		@PathVariable Long roomId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(chatService.getRoom(roomId, currentUserId(userDetails))));
	}

	@Operation(summary = "피드별 채팅방 조회")
	@GetMapping("/rooms/by-feed/{feedId}")
	public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getRoomsByFeed(
		@PathVariable String feedId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(chatService.getRoomsByFeed(feedId, currentUserId(userDetails))));
	}

	@Operation(summary = "스팟별 채팅방 조회")
	@GetMapping("/rooms/by-spot/{spotId}")
	public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getRoomBySpot(
		@PathVariable String spotId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(chatService.getRoomsBySpot(spotId, currentUserId(userDetails))));
	}

	@Operation(
		summary = "채팅방 멤버 목록 조회",
		description = "피드 단계든 스팟 단계든 관계없이 해당 채팅방에 속한 멤버 전체를 반환합니다. 본인이 멤버인 방만 조회 가능합니다."
	)
	@GetMapping("/rooms/{roomId}/members")
	public ResponseEntity<ApiResponse<List<ChatMemberResponse>>> getMembers(
		@PathVariable Long roomId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(chatService.getMembers(roomId, currentUserId(userDetails))));
	}

	@Operation(summary = "채팅방 사진 모아보기", description = "방에 공유된 IMAGE 타입 메시지를 최신순으로 반환합니다.")
	@GetMapping("/rooms/{roomId}/photos")
	public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getPhotos(
		@PathVariable Long roomId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(chatService.getPhotos(roomId, currentUserId(userDetails))));
	}

	// ─── 메시지 (Message) ─────────────────────────

	@Operation(
		summary = "채팅 메시지 조회 (커서 기반)",
		description = "cursor 없으면 최신 메시지부터, cursor 있으면 해당 ID 이전 메시지를 size 개 반환합니다."
	)
	@GetMapping("/rooms/{roomId}/messages")
	public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getMessages(
		@PathVariable Long roomId,
		@Parameter(description = "커서 (마지막 메시지 ID 문자열, 최초 조회 시 생략)")
		@RequestParam(required = false) Long cursor,
		@RequestParam(defaultValue = "30") int size,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		ChatMessageListResponse.Result result =
			chatService.getMessages(roomId, cursor, size, currentUserId(userDetails));
		return ResponseEntity.ok(ApiResponse.success(result.data(), result.meta()));
	}

	@Operation(
		summary = "메시지 전송",
		description = "메시지를 DB에 저장하고 해당 채팅방을 구독 중인 모든 클라이언트에게 SSE로 브로드캐스트합니다."
	)
	@PostMapping("/rooms/{roomId}/messages")
	public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
		@PathVariable Long roomId,
		@Valid @RequestBody SendMessageRequest request,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(
			chatService.sendMessage(roomId, request, currentUserId(userDetails))
		));
	}

	@Operation(
		summary = "타이핑 이벤트 전송",
		description = "입력 중 상태를 같은 방 구독자에게 SSE로 브로드캐스트합니다. DB 저장 없음."
	)
	@PostMapping("/rooms/{roomId}/typing")
	public ResponseEntity<ApiResponse<Void>> typing(
		@PathVariable Long roomId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		chatService.broadcastTyping(roomId, currentUserId(userDetails));
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(
		summary = "채팅방 전체 읽음 처리",
		description = "본 채팅방의 모든 메시지를 읽음 처리합니다 (lastReadMessageId 를 최신 메시지 ID 로 끌어올림). "
			+ "특정 메시지까지만 읽음 처리하려면 POST /rooms/{roomId}/messages/{messageId}/read 를 사용하세요."
	)
	@PostMapping("/rooms/{roomId}/read")
	public ResponseEntity<ApiResponse<Void>> markAsRead(
		@PathVariable Long roomId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		chatService.markAsRead(roomId, currentUserId(userDetails));
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(
		summary = "특정 메시지까지 읽음 처리",
		description = "클라이언트가 \"여기까지 봤어요\" 를 명시적으로 보고합니다. "
			+ "lastReadMessageId 는 단조 증가 — 더 작은 messageId 호출은 무시됩니다 (멱등). "
			+ "lastReadMessageId 가 실제로 전진한 경우에만 SSE READ 이벤트가 동일 방 구독자에게 "
			+ "lastReadMessageId 와 함께 브로드캐스트됩니다. 중복/순서 어긋난 호출에서는 broadcast 가 생략됩니다."
	)
	@PostMapping("/rooms/{roomId}/messages/{messageId}/read")
	public ResponseEntity<ApiResponse<Void>> markMessageAsRead(
		@PathVariable Long roomId,
		@PathVariable Long messageId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		chatService.markAsReadUpTo(roomId, messageId, currentUserId(userDetails));
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(
		summary = "채팅방 나가기",
		description = "현재 사용자를 채팅방 멤버에서 제거합니다. "
			+ "GROUP 방은 \"OO님이 나갔습니다.\" SYSTEM 메시지가 SSE 로 브로드캐스트되고, "
			+ "PERSONAL 방은 조용히 나갑니다. 나간 후 멤버가 0 명이면 방이 soft-delete 됩니다. "
			+ "비멤버 호출은 403 CH003."
	)
	@DeleteMapping("/rooms/{roomId}/members/me")
	public ResponseEntity<ApiResponse<Void>> leaveRoom(
		@PathVariable Long roomId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		chatService.leaveRoom(roomId, currentUserId(userDetails));
		return ResponseEntity.ok(ApiResponse.success());
	}

	// ─── 방 알림 설정 (Notification mute) ──────────

	@Operation(summary = "채팅방 알림 설정 조회", description = "현재 사용자의 이 방에 대한 알림 수신 여부를 반환합니다.")
	@GetMapping("/rooms/{roomId}/notification")
	public ResponseEntity<ApiResponse<ChatNotificationSettingResponse>> getRoomNotification(
		@PathVariable Long roomId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(
			chatService.getRoomNotification(roomId, currentUserId(userDetails))
		));
	}

	@Operation(summary = "채팅방 알림 끄기/켜기", description = "현재 사용자의 이 방에 대한 알림 수신 여부를 변경합니다. (enabled=false 음소거)")
	@PatchMapping("/rooms/{roomId}/notification")
	public ResponseEntity<ApiResponse<ChatNotificationSettingResponse>> updateRoomNotification(
		@PathVariable Long roomId,
		@Valid @RequestBody UpdateChatNotificationRequest request,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(
			chatService.updateRoomNotification(roomId, currentUserId(userDetails), request.getEnabled())
		));
	}

	// ─── 투표 (Vote) ──────────────────────────────

	@Operation(
		summary = "채팅방 투표 목록 조회",
		description = "채팅방에 생성된 투표를 최신순으로 반환합니다. 카카오톡 투표 탭과 동일한 목록입니다."
	)
	@GetMapping("/rooms/{roomId}/votes")
	public ResponseEntity<ApiResponse<List<ChatVoteResponse>>> getVotes(
		@PathVariable Long roomId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		chatService.assertMembershipPublic(roomId, currentUserId(userDetails));
		return ResponseEntity.ok(ApiResponse.success(
			chatVoteService.getVotes(roomId, currentUserId(userDetails))
		));
	}

	@Operation(
		summary = "채팅 투표 생성",
		description = "채팅방 멤버만 생성 가능. 생성 시 채팅방 구독자 전원에게 vote_created SSE 이벤트가 전송됩니다."
	)
	@PostMapping("/rooms/{roomId}/votes")
	public ResponseEntity<ApiResponse<ChatVoteResponse>> createVote(
		@PathVariable Long roomId,
		@Valid @RequestBody CreateChatVoteRequest request,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		chatService.assertMembershipPublic(roomId, currentUserId(userDetails));
		return ResponseEntity.ok(ApiResponse.success(
			chatVoteService.createVote(roomId, request, currentUserId(userDetails))
		));
	}

	@Operation(
		summary = "투표 참여 (단건 토글)",
		description = "같은 선택지 재캐스트 = 투표 해제. 단일선택 시 기존 표는 자동 교체됩니다. "
			+ "변경 후 vote_updated SSE 이벤트가 브로드캐스트됩니다."
	)
	@PostMapping("/rooms/{roomId}/votes/{voteId}/cast")
	public ResponseEntity<ApiResponse<ChatVoteResponse>> castVote(
		@PathVariable Long roomId,
		@PathVariable Long voteId,
		@Valid @RequestBody CastChatVoteRequest request,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		chatService.assertMembershipPublic(roomId, currentUserId(userDetails));
		return ResponseEntity.ok(ApiResponse.success(
			chatVoteService.castVote(roomId, voteId, request, currentUserId(userDetails))
		));
	}

	@Operation(
		summary = "투표 일괄 제출 (최종 확정)",
		description = "optionIds 를 최종 확정 상태로 전달. 서버가 현재 답변과 diff 하여 원자적으로 적용합니다. "
			+ "빈 배열 [] = 전체 취소. 멱등."
	)
	@PutMapping("/rooms/{roomId}/votes/{voteId}/submit")
	public ResponseEntity<ApiResponse<ChatVoteResponse>> submitVoteAnswers(
		@PathVariable Long roomId,
		@PathVariable Long voteId,
		@Valid @RequestBody SubmitChatVoteAnswersRequest request,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		chatService.assertMembershipPublic(roomId, currentUserId(userDetails));
		return ResponseEntity.ok(ApiResponse.success(
			chatVoteService.submitAnswers(roomId, voteId, request, currentUserId(userDetails))
		));
	}

	@Operation(
		summary = "투표 마감 (생성자 전용)",
		description = "투표를 CLOSED 상태로 전환합니다. 마감 후에는 참여 불가. "
			+ "vote_closed SSE 이벤트가 브로드캐스트됩니다."
	)
	@PostMapping("/rooms/{roomId}/votes/{voteId}/close")
	public ResponseEntity<ApiResponse<ChatVoteResponse>> closeVote(
		@PathVariable Long roomId,
		@PathVariable Long voteId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		chatService.assertMembershipPublic(roomId, currentUserId(userDetails));
		return ResponseEntity.ok(ApiResponse.success(
			chatVoteService.closeVote(roomId, voteId, currentUserId(userDetails))
		));
	}

	@Operation(
		summary = "선택지 추가 (생성자 전용)",
		description = "ACTIVE 투표에 새 선택지를 추가합니다. 추가 후 vote_updated SSE 이벤트가 브로드캐스트됩니다."
	)
	@PostMapping("/rooms/{roomId}/votes/{voteId}/options")
	public ResponseEntity<ApiResponse<ChatVoteResponse>> addVoteOption(
		@PathVariable Long roomId,
		@PathVariable Long voteId,
		@Valid @RequestBody AddChatVoteOptionRequest request,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		chatService.assertMembershipPublic(roomId, currentUserId(userDetails));
		return ResponseEntity.ok(ApiResponse.success(
			chatVoteService.addOption(roomId, voteId, request, currentUserId(userDetails))
		));
	}

	@Operation(
		summary = "선택지 삭제 (생성자 전용)",
		description = "ACTIVE 투표의 선택지를 삭제합니다. 해당 선택지에 투표한 답변도 함께 삭제됩니다. "
			+ "삭제 후 vote_updated SSE 이벤트가 브로드캐스트됩니다."
	)
	@DeleteMapping("/rooms/{roomId}/votes/{voteId}/options/{optionId}")
	public ResponseEntity<ApiResponse<ChatVoteResponse>> removeVoteOption(
		@PathVariable Long roomId,
		@PathVariable Long voteId,
		@PathVariable Long optionId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		chatService.assertMembershipPublic(roomId, currentUserId(userDetails));
		return ResponseEntity.ok(ApiResponse.success(
			chatVoteService.removeOption(roomId, voteId, optionId, currentUserId(userDetails))
		));
	}

	// ─── 차단 (Block) ─────────────────────────────

	@Operation(
		summary = "내가 차단한 유저 목록",
		description = "본인이 차단한 유저 목록을 최신순으로 반환합니다. 닉네임이 함께 제공됩니다."
	)
	@GetMapping("/blocks")
	public ResponseEntity<ApiResponse<List<ChatBlockResponse>>> getBlocks(
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(chatService.getBlocks(currentUserId(userDetails))));
	}

	@Operation(
		summary = "유저 차단",
		description = "지정한 유저를 차단합니다 (멱등 — 이미 차단된 경우 기존 row 반환). "
			+ "차단 후 PERSONAL 방의 새 메시지는 placeholder 로 가려지고, 두 유저 간 새 PERSONAL 방 시작은 차단됩니다 (CH009). "
			+ "GROUP 방 메시지는 영향 없음."
	)
	@PostMapping("/blocks")
	public ResponseEntity<ApiResponse<ChatBlockResponse>> blockUser(
		@Valid @RequestBody CreateChatBlockRequest request,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(
			chatService.blockUser(currentUserId(userDetails), request.getUserId())
		));
	}

	@Operation(
		summary = "유저 차단 해제",
		description = "차단을 해제합니다. 차단한 적 없는 유저를 해제해도 멱등 no-op (200)."
	)
	@DeleteMapping("/blocks/{userId}")
	public ResponseEntity<ApiResponse<Void>> unblockUser(
		@PathVariable String userId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		chatService.unblockUser(currentUserId(userDetails), userId);
		return ResponseEntity.ok(ApiResponse.success());
	}

	private String currentUserId(CustomUserDetails userDetails) {
		if (userDetails == null || userDetails.getUserId() == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
		}
		return userDetails.getUserId();
	}
}
