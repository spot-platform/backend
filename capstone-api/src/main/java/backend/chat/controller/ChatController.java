package backend.chat.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import backend.chat.dto.ChatBlockResponse;
import backend.chat.dto.ChatMessageListResponse;
import backend.chat.dto.ChatMessageResponse;
import backend.chat.dto.ChatRoomResponse;
import backend.chat.dto.CreateChatBlockRequest;
import backend.chat.dto.CreateChatRoomRequest;
import backend.chat.dto.CreatePersonalChatRoomRequest;
import backend.chat.dto.SendMessageRequest;
import backend.chat.service.ChatService;
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
	private final SseEmitterService sseEmitterService;

	// ─── SSE 연결 ─────────────────────────────────

	@Operation(
		summary = "SSE 구독 연결",
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

	// ─── 채팅방 (Room) ─────────────────────────────

	@Operation(summary = "채팅방 목록 조회")
	@GetMapping("/rooms")
	public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getRooms(
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(chatService.getRooms(currentUserId(userDetails))));
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

	@Operation(summary = "스팟별 채팅방 조회")
	@GetMapping("/rooms/by-spot/{spotId}")
	public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getRoomBySpot(
		@PathVariable String spotId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(chatService.getRoomsBySpot(spotId, currentUserId(userDetails))));
	}

	@Operation(summary = "사용자별 채팅방 조회")
	@GetMapping("/rooms/by-user/{userId}")
	public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getRoomsByUser(
		@PathVariable String userId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(chatService.getRoomsByUser(userId, currentUserId(userDetails))));
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

	/**
	 * @AuthenticationPrincipal 으로 받은 CustomUserDetails 에서 userId 를 꺼낸다.
	 * 비인증 (Anonymous 토큰) 의 경우 Spring 이 null 을 주입하므로 null 안전.
	 */
	private String currentUserId(CustomUserDetails userDetails) {
		return userDetails == null ? null : userDetails.getUserId();
	}
}
