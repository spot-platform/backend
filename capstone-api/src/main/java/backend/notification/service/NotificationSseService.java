package backend.notification.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import backend.notification.event.NotificationCreatedEvent;
import lombok.extern.slf4j.Slf4j;

/**
 * 유저 ID 기준으로 SSE Emitter를 관리하는 서비스.
 *
 * <p>채팅의 SseEmitterService와 동일한 패턴이며, roomId 대신 userId(String)를 키로 사용한다.
 * 단일 서버 인메모리 방식이며, 스케일아웃 환경에서는 Redis Pub/Sub으로 교체할 것을 권장한다.
 */
@Slf4j
@Service
public class NotificationSseService {

	private static final long SSE_TIMEOUT_MS = 10 * 60 * 1000L;

	private final Map<String, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

	public SseEmitter subscribe(String userId) {
		SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

		userEmitters.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(emitter);

		emitter.onCompletion(() -> removeEmitter(userId, emitter));
		emitter.onTimeout(() -> {
			log.debug("[SSE] timeout - userId={}", userId);
			removeEmitter(userId, emitter);
		});
		emitter.onError(e -> {
			log.debug("[SSE] error - userId={}, error={}", userId, e.getMessage());
			removeEmitter(userId, emitter);
		});

		sendPing(emitter, userId);

		log.debug("[SSE] subscribed - userId={}", userId);
		return emitter;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onNotificationCreated(NotificationCreatedEvent event) {
		send(event.userId(), event.notification());
	}

	/**
	 * DB 저장 없이 SSE만 전송. 채팅 메시지처럼 알림 목록을 오염시키지 않아야 하는 실시간 이벤트에 사용.
	 * "notification" 이벤트와 스키마 혼용을 방지하기 위해 "chat-notification" 이벤트 name으로 분리 전송.
	 * payload: {"roomId": ..., "message": "..."}
	 * 프론트에서 roomId 기반으로 "현재 그 방을 보고 있으면 팝업 무시" 분기 처리 가능.
	 */
	public void pushOnly(String userId, Long roomId, String message) {
		List<SseEmitter> emitters = userEmitters.getOrDefault(userId, List.of());
		if (emitters.isEmpty()) {
			return;
		}
		SseEmitter.SseEventBuilder event = SseEmitter.event()
			.name("chat-notification")
			.data(java.util.Map.of("roomId", roomId, "message", message));
		for (SseEmitter emitter : emitters) {
			try {
				emitter.send(event);
			} catch (IOException e) {
				log.debug("[SSE] chat-notification 전송 실패 - userId={}", userId, e);
				removeEmitter(userId, emitter);
				emitter.completeWithError(e);
			}
		}
	}

	private void send(String userId, Object data) {
		List<SseEmitter> emitters = userEmitters.getOrDefault(userId, List.of());

		if (emitters.isEmpty()) {
			return;
		}

		SseEmitter.SseEventBuilder event = SseEmitter.event()
			.name("notification")
			.data(data);

		for (SseEmitter emitter : emitters) {
			try {
				emitter.send(event);
			} catch (IOException e) {
				log.debug("[SSE] send failed - userId={}, removing emitter", userId, e);
				removeEmitter(userId, emitter);
				emitter.completeWithError(e);
			}
		}
	}

	private void removeEmitter(String userId, SseEmitter emitter) {
		userEmitters.compute(userId, (id, list) -> {
			if (list == null) {
				return null;
			}
			list.remove(emitter);
			return list.isEmpty() ? null : list;
		});
	}

	private void sendPing(SseEmitter emitter, String userId) {
		try {
			emitter.send(SseEmitter.event().name("ping").data("connected"));
		} catch (IOException e) {
			log.debug("[SSE] initial ping failed - userId={}", userId);
			removeEmitter(userId, emitter);
			emitter.completeWithError(e);
		}
	}
}
