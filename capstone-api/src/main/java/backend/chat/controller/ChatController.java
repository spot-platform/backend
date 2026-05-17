package backend.chat.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import backend.chat.dto.ChatMessageListResponse;
import backend.chat.dto.ChatMessageResponse;
import backend.chat.dto.ChatRoomResponse;
import backend.chat.dto.CreateChatRoomRequest;
import backend.chat.dto.CreatePersonalChatRoomRequest;
import backend.chat.dto.SendMessageRequest;
import backend.chat.service.ChatService;
import backend.chat.service.SseEmitterService;
import backend.global.common.response.ApiResponse;
import backend.global.security.CustomUserDetails;
import backend.user.entity.UserEntity;
import backend.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Chat API", description = "채팅 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

	private final ChatService chatService;
	private final SseEmitterService sseEmitterService;
	private final UserRepository userRepository;

	// ─── SSE 연결 ─────────────────────────────────

	@Operation(
		summary = "SSE 구독 연결",
		description = "특정 채팅방에 SSE 실시간 연결을 맺습니다. 연결 후 해당 방에 새 메시지가 오면 즉시 수신됩니다."
	)
	@GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter connect(
		@Parameter(description = "구독할 채팅방 ID", required = true)
		@RequestParam Long roomId
	) {
		return sseEmitterService.subscribe(roomId);
	}

	// ─── 채팅방 (Room) ─────────────────────────────

	@Operation(summary = "채팅방 목록 조회")
	@GetMapping("/rooms")
	public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getRooms(
		@AuthenticationPrincipal Object principal
	) {
		return ResponseEntity.ok(ApiResponse.success(chatService.getRooms(currentUserId(principal))));
	}

	@Operation(
		summary = "그룹 채팅방 생성",
		description = "GROUP 타입만 허용. 동일 spotId 의 활성 GROUP 방이 있으면 idempotent 하게 기존 방을 반환합니다. "
			+ "PERSONAL 채팅은 POST /rooms/personal 을 사용하세요."
	)
	@PostMapping("/rooms")
	public ResponseEntity<ApiResponse<ChatRoomResponse>> createRoom(
		@Valid @RequestBody CreateChatRoomRequest request,
		@AuthenticationPrincipal Object principal
	) {
		return ResponseEntity.ok(ApiResponse.success(
			chatService.createRoom(request, currentUserId(principal))
		));
	}

	@Operation(
		summary = "1:1 채팅방 시작",
		description = "현재 유저와 partnerId 사이의 PERSONAL 채팅방을 만들거나 기존 방을 반환합니다 (카카오톡 스타일)."
	)
	@PostMapping("/rooms/personal")
	public ResponseEntity<ApiResponse<ChatRoomResponse>> createPersonalRoom(
		@Valid @RequestBody CreatePersonalChatRoomRequest request,
		@AuthenticationPrincipal Object principal
	) {
		return ResponseEntity.ok(ApiResponse.success(
			chatService.createPersonalRoom(request, currentUserId(principal))
		));
	}

	@Operation(summary = "채팅방 상세 조회")
	@GetMapping("/rooms/{roomId}")
	public ResponseEntity<ApiResponse<ChatRoomResponse>> getRoom(
		@PathVariable Long roomId,
		@AuthenticationPrincipal Object principal
	) {
		return ResponseEntity.ok(ApiResponse.success(chatService.getRoom(roomId, currentUserId(principal))));
	}

	@Operation(summary = "스팟별 채팅방 조회")
	@GetMapping("/rooms/by-spot/{spotId}")
	public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getRoomBySpot(
		@PathVariable String spotId,
		@AuthenticationPrincipal Object principal
	) {
		return ResponseEntity.ok(ApiResponse.success(chatService.getRoomsBySpot(spotId, currentUserId(principal))));
	}

	@Operation(summary = "사용자별 채팅방 조회")
	@GetMapping("/rooms/by-user/{userId}")
	public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getRoomsByUser(
		@PathVariable String userId,
		@AuthenticationPrincipal Object principal
	) {
		return ResponseEntity.ok(ApiResponse.success(chatService.getRoomsByUser(userId, currentUserId(principal))));
	}

	// ─── 메시지 (Message) ─────────────────────────

	@Operation(
		summary = "채팅 메시지 조회 (커서 기반)",
		description = "cursor 없으면 최신 메시지부터, cursor 있으면 해당 ID 이전 메시지를 size 개 반환합니다."
	)
	@GetMapping("/rooms/{roomId}/messages")
	public ResponseEntity<ApiResponse<ChatMessageListResponse>> getMessages(
		@PathVariable Long roomId,
		@Parameter(description = "커서 (마지막 메시지 ID, 최초 조회 시 생략)")
		@RequestParam(required = false) Long cursor,
		@RequestParam(defaultValue = "30") int size,
		@AuthenticationPrincipal Object principal
	) {
		return ResponseEntity.ok(ApiResponse.success(
			chatService.getMessages(roomId, cursor, size, currentUserId(principal))
		));
	}

	@Operation(
		summary = "메시지 전송",
		description = "메시지를 DB에 저장하고 해당 채팅방을 구독 중인 모든 클라이언트에게 SSE로 브로드캐스트합니다."
	)
	@PostMapping("/rooms/{roomId}/messages")
	public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
		@PathVariable Long roomId,
		@Valid @RequestBody SendMessageRequest request,
		@AuthenticationPrincipal Object principal
	) {
		return ResponseEntity.ok(ApiResponse.success(
			chatService.sendMessage(roomId, request, currentUserId(principal))
		));
	}

	@Operation(summary = "메시지 읽음 처리")
	@PostMapping("/rooms/{roomId}/read")
	public ResponseEntity<ApiResponse<Void>> markAsRead(
		@PathVariable Long roomId,
		@AuthenticationPrincipal Object principal
	) {
		chatService.markAsRead(roomId, currentUserId(principal));
		return ResponseEntity.ok(ApiResponse.success());
	}

	/**
	 * SecurityContext 의 principal 을 실제 user ID 로 변환.
	 *
	 * <p>JWTFilter 가 현재 principal 에 email (String) 만 셋팅하므로 (createSpot/Chat 등에서
	 * @AuthenticationPrincipal CustomUserDetails 가 null 로 들어오는 원인) 여기서 한 번 더
	 * UserRepository.findByEmail 을 거쳐 진짜 userId 로 풀어준다. CustomUserDetails 가 들어오는
	 * 경로 (Login 직후 등) 도 함께 처리하기 위해 Object 로 받아 type 분기.
	 *
	 * <p>TODO: 별도 PR 에서 JWTFilter 자체가 principal 에 CustomUserDetails 를 셋팅하도록
	 *   리팩터링되면 본 헬퍼는 제거.
	 */
	private String currentUserId(Object principal) {
		if (principal == null || "anonymousUser".equals(principal)) {
			return null;
		}
		if (principal instanceof CustomUserDetails userDetails) {
			return userDetails.getUserId();
		}
		if (principal instanceof String email) {
			return userRepository.findByEmail(email)
				.map(UserEntity::getId)
				.orElse(null);
		}
		return null;
	}
}
