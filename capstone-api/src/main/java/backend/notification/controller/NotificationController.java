package backend.notification.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import backend.global.common.response.ApiResponse;
import backend.global.security.CustomUserDetails;
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
		description = "로그인한 유저의 SSE 연결을 맺습니다. 연결 후 해당 유저에게 알림 발생 시 즉시 수신됩니다."
	)
	@GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter subscribe(@AuthenticationPrincipal CustomUserDetails userDetails) {
		return notificationSseService.subscribe(currentUserId(userDetails));
	}

	@Operation(summary = "알림 목록 조회", description = "로그인한 유저의 알림 목록을 최신순으로 반환합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		return ResponseEntity.ok(ApiResponse.success(notificationService.getNotifications(currentUserId(userDetails))));
	}

	@Operation(summary = "알림 읽음 처리")
	@PatchMapping("/{notificationId}/read")
	public ResponseEntity<ApiResponse<Void>> markAsRead(
		@PathVariable String notificationId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		notificationService.markAsRead(currentUserId(userDetails), notificationId);
		return ResponseEntity.ok(ApiResponse.success());
	}

	private String currentUserId(CustomUserDetails userDetails) {
		if (userDetails == null || userDetails.getUserId() == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
		}
		return userDetails.getUserId();
	}
}
