package backend.chat.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import backend.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Chat API", description = "채팅 API")
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

	@Operation(summary = "채팅방 목록 조회")
	@GetMapping("/rooms")
	public ResponseEntity<ApiResponse<Void>> getRooms() {
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "채팅방 생성")
	@PostMapping("/rooms")
	public ResponseEntity<ApiResponse<Void>> createRoom() {
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "채팅방 상세 조회")
	@GetMapping("/rooms/{roomId}")
	public ResponseEntity<ApiResponse<Void>> getRoom(@PathVariable Long roomId) {
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "채팅 메시지 조회")
	@GetMapping("/rooms/{roomId}/messages")
	public ResponseEntity<ApiResponse<Void>> getMessages(
		@PathVariable Long roomId,
		@RequestParam(required = false) Long cursor,
		@RequestParam(defaultValue = "30") int size
	) {
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "메시지 전송")
	@PostMapping("/rooms/{roomId}/messages")
	public ResponseEntity<ApiResponse<Void>> sendMessage(@PathVariable Long roomId) {
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "메시지 읽음 처리")
	@PostMapping("/rooms/{roomId}/read")
	public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long roomId) {
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "스팟별 채팅방 조회")
	@GetMapping("/rooms/by-spot/{spotId}")
	public ResponseEntity<ApiResponse<Void>> getRoomBySpot(@PathVariable String spotId) {
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "사용자별 채팅방 조회")
	@GetMapping("/rooms/by-user/{userId}")
	public ResponseEntity<ApiResponse<Void>> getRoomsByUser(@PathVariable String userId) {
		return ResponseEntity.ok(ApiResponse.success());
	}

	@Operation(summary = "SSE 연결")
	@GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter connect() {
		return new SseEmitter();
	}
}
