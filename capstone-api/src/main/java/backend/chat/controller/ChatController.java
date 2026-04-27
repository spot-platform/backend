package backend.chat.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import backend.chat.dto.SendMessageRequest;
import backend.chat.service.ChatService;
import backend.chat.service.SseEmitterService;
import backend.global.common.response.ApiResponse;
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
	public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getRooms() {
		return ResponseEntity.ok(ApiResponse.success(chatService.getRooms()));
	}

	@Operation(summary = "채팅방 생성")
	@PostMapping("/rooms")
	public ResponseEntity<ApiResponse<ChatRoomResponse>> createRoom(
		@Valid @RequestBody CreateChatRoomRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success(chatService.createRoom(request)));
	}

	@Operation(summary = "채팅방 상세 조회")
	@GetMapping("/rooms/{roomId}")
	public ResponseEntity<ApiResponse<ChatRoomResponse>> getRoom(@PathVariable Long roomId) {
		return ResponseEntity.ok(ApiResponse.success(chatService.getRoom(roomId)));
	}

	@Operation(summary = "스팟별 채팅방 조회")
	@GetMapping("/rooms/by-spot/{spotId}")
	public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getRoomBySpot(
		@PathVariable String spotId
	) {
		return ResponseEntity.ok(ApiResponse.success(chatService.getRoomsBySpot(spotId)));
	}

	@Operation(summary = "사용자별 채팅방 조회")
	@GetMapping("/rooms/by-user/{userId}")
	public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getRoomsByUser(
		@PathVariable String userId
	) {
		return ResponseEntity.ok(ApiResponse.success(chatService.getRoomsByUser(userId)));
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
		@RequestParam(defaultValue = "30") int size
	) {
		return ResponseEntity.ok(ApiResponse.success(chatService.getMessages(roomId, cursor, size)));
	}

	@Operation(
		summary = "메시지 전송",
		description = "메시지를 DB에 저장하고 해당 채팅방을 구독 중인 모든 클라이언트에게 SSE로 브로드캐스트합니다."
	)
	@PostMapping("/rooms/{roomId}/messages")
	public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(
		@PathVariable Long roomId,
		@Valid @RequestBody SendMessageRequest request
	) {
		return ResponseEntity.ok(ApiResponse.success(chatService.sendMessage(roomId, request)));
	}

	@Operation(summary = "메시지 읽음 처리")
	@PostMapping("/rooms/{roomId}/read")
	public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long roomId) {
		chatService.markAsRead(roomId);
		return ResponseEntity.ok(ApiResponse.success());
	}
}
