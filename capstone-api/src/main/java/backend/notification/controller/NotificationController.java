package backend.notification.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import backend.global.common.response.ApiResponse;
import backend.notification.dto.NotificationResponse;
import backend.notification.service.NotificationService;
import backend.notification.service.NotificationSseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Notification API", description = "알림 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;
	private final NotificationSseService notificationSseService;

	@Operation(
		summary = "SSE 알림 구독",
		description = "userId로 SSE 연결을 맺습니다. 연결 후 해당 유저에게 알림 발생 시 즉시 수신됩니다."
	)
	@GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter subscribe(@RequestParam String userId) {
		return notificationSseService.subscribe(userId);
	}

	@Operation(summary = "알림 목록 조회", description = "userId에 해당하는 알림 목록을 최신순으로 반환합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(@RequestParam String userId) {
		return ResponseEntity.ok(ApiResponse.success(notificationService.getNotifications(userId)));
	}

	@Operation(summary = "알림 읽음 처리")
	@PatchMapping("/{notificationId}/read")
	public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable String notificationId) {
		notificationService.markAsRead(notificationId);
		return ResponseEntity.ok(ApiResponse.success());
	}
}
