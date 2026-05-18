package backend.notification.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
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
@RequestMapping("/api/v1/notifications")
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

	@Operation(summary = "알림 목록 조회", description = "로그인한 유저의 알림 목록을 최신순으로 페이지 단위로 반환합니다.")
	@GetMapping
	public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
		@AuthenticationPrincipal CustomUserDetails userDetails,
		@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		return ResponseEntity.ok(ApiResponse.success(notificationService.getNotifications(currentUserId(userDetails), pageable)));
	}

	@Operation(summary = "알림 읽음 처리")
	@PostMapping("/{notificationId}/read")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void markAsRead(
		@PathVariable String notificationId,
		@AuthenticationPrincipal CustomUserDetails userDetails
	) {
		notificationService.markAsRead(currentUserId(userDetails), notificationId);
	}

	@Operation(summary = "전체 알림 읽음 처리 (미구현)")
	@PostMapping("/read-all")
	public void markAllAsRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
		currentUserId(userDetails);
		throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
			"전체 알림 읽음 처리는 아직 구현되지 않았습니다.");
	}

	private String currentUserId(CustomUserDetails userDetails) {
		if (userDetails == null || userDetails.getUserId() == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다.");
		}
		return userDetails.getUserId();
	}
}
