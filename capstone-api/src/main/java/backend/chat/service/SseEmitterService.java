package backend.chat.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import backend.chat.dto.ChatMessageResponse;
import backend.chat.dto.ChatSseEvent;
import backend.chat.repository.ChatRoomRepository;
import backend.global.error.exception.BusinessException;
import backend.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SSE(Server-Sent Events) 구독·브로드캐스트를 담당하는 서비스.
 *
 * <p>두 종류의 SSE 채널을 관리한다:
 * <ul>
 *   <li><b>방(room) 채널</b> — roomId 기준. 채팅방 내 실시간 메시지/읽음/타이핑 전달.</li>
 *   <li><b>유저(user) 채널</b> — userId 기준. 채팅 목록 화면의 unread 배지 실시간 갱신.</li>
 * </ul>
 *
 * <p>모든 아웃바운드 이벤트는 {@link ChatEventPublisher} 를 통해 Redis Pub/Sub 채널로 발행되며,
 * {@link ChatRedisSubscriber} 가 이를 수신해 {@link #sendRoomEventLocal} /
 * {@link #sendUserEventLocal} 로 로컬 SSE 연결에 전달한다.
 * 이 구조 덕분에 다중 서버 환경에서도 모든 인스턴스의 클라이언트가 이벤트를 수신한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SseEmitterService {

	/** SSE 연결 유지 시간: 10분 */
	private static final long SSE_TIMEOUT_MS = 10 * 60 * 1000L;

	/** roomId → 해당 방을 구독 중인 Emitter 목록 */
	private final Map<Long, List<SseEmitter>> roomEmitters = new ConcurrentHashMap<>();

	/** userId → 유저 레벨 Emitter 목록 (채팅 목록 화면 배지 갱신용) */
	private final Map<String, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

	private final ChatRoomRepository chatRoomRepository;
	private final ChatEventPublisher chatEventPublisher;

	// ──────────────────────────────────────────────
	// 구독 (Subscribe)
	// ──────────────────────────────────────────────

	/** 특정 채팅방 SSE 구독. 멤버십 검증은 Controller 에서 선행한다. */
	public SseEmitter subscribe(Long roomId) {
		chatRoomRepository.findById(roomId)
			.orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

		SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
		roomEmitters.computeIfAbsent(roomId, id -> new CopyOnWriteArrayList<>()).add(emitter);

		emitter.onCompletion(() -> removeRoomEmitter(roomId, emitter));
		emitter.onTimeout(() -> {
			log.debug("[SSE] room timeout - roomId={}", roomId);
			removeRoomEmitter(roomId, emitter);
		});
		emitter.onError(e -> {
			log.debug("[SSE] room error - roomId={}, err={}", roomId, e.getMessage());
			removeRoomEmitter(roomId, emitter);
		});

		sendPing(emitter, "room", roomId);
		log.debug("[SSE] room subscribed - roomId={}", roomId);
		return emitter;
	}

	/**
	 * 유저 레벨 SSE 구독. 채팅 목록 화면에서 사용.
	 * 다른 방에 새 메시지가 왔을 때 unread 배지를 실시간으로 갱신한다.
	 */
	public SseEmitter subscribeUser(String userId) {
		SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
		userEmitters.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>()).add(emitter);

		emitter.onCompletion(() -> removeUserEmitter(userId, emitter));
		emitter.onTimeout(() -> {
			log.debug("[SSE] user timeout - userId={}", userId);
			removeUserEmitter(userId, emitter);
		});
		emitter.onError(e -> {
			log.debug("[SSE] user error - userId={}, err={}", userId, e.getMessage());
			removeUserEmitter(userId, emitter);
		});

		sendPing(emitter, "user", userId);
		log.debug("[SSE] user subscribed - userId={}", userId);
		return emitter;
	}

	// ──────────────────────────────────────────────
	// 브로드캐스트 (Broadcast) — Redis 경유
	// ──────────────────────────────────────────────

	/** 채팅방에 새 메시지를 Redis Pub/Sub 으로 발행. 모든 인스턴스의 SSE 클라이언트가 수신. */
	public void broadcast(Long roomId, ChatMessageResponse message) {
		chatEventPublisher.publishRoom(roomId, ChatSseEvent.message(message));
	}

	public void broadcastRead(Long roomId, String userId) {
		broadcastRead(roomId, userId, null);
	}

	public void broadcastRead(Long roomId, String userId, Long lastReadMessageId) {
		chatEventPublisher.publishRoom(roomId, ChatSseEvent.read(roomId, userId, lastReadMessageId));
	}

	public void broadcastTyping(Long roomId, String userId) {
		chatEventPublisher.publishRoom(roomId, ChatSseEvent.typing(roomId, userId));
	}

	/**
	 * 유저 레벨 SSE 채널로 unread 배지 갱신 이벤트를 Redis 를 통해 발행.
	 * 채팅 목록 화면에서 구독 중인 유저에게 해당 방의 배지를 갱신하도록 알린다.
	 */
	public void broadcastBadgeUpdate(String userId, Long roomId, long unreadCount) {
		chatEventPublisher.publishUser(userId, ChatSseEvent.badgeUpdate(roomId, unreadCount));
	}

	// ──────────────────────────────────────────────
	// 로컬 전송 (Local Send) — ChatRedisSubscriber 에서 호출
	// ──────────────────────────────────────────────

	/** Redis 구독자가 수신한 방 이벤트를 로컬 SSE 연결에 전달한다. */
	public void sendRoomEventLocal(Long roomId, ChatSseEvent event) {
		List<SseEmitter> emitters = roomEmitters.getOrDefault(roomId, List.of());
		if (emitters.isEmpty()) {
			return;
		}
		SseEmitter.SseEventBuilder sseEvent = SseEmitter.event()
			.name(event.getType().getValue())
			.data(event);
		for (SseEmitter emitter : emitters) {
			try {
				emitter.send(sseEvent);
			} catch (IOException e) {
				log.debug("[SSE] room send failed - roomId={}", roomId);
				removeRoomEmitter(roomId, emitter);
			}
		}
	}

	/** Redis 구독자가 수신한 유저 이벤트를 로컬 SSE 연결에 전달한다. */
	public void sendUserEventLocal(String userId, ChatSseEvent event) {
		List<SseEmitter> emitters = userEmitters.getOrDefault(userId, List.of());
		if (emitters.isEmpty()) {
			return;
		}
		SseEmitter.SseEventBuilder sseEvent = SseEmitter.event()
			.name(event.getType().getValue())
			.data(event);
		for (SseEmitter emitter : emitters) {
			try {
				emitter.send(sseEvent);
			} catch (IOException e) {
				log.debug("[SSE] user send failed - userId={}", userId);
				removeUserEmitter(userId, emitter);
			}
		}
	}

	// ──────────────────────────────────────────────
	// 내부 헬퍼
	// ──────────────────────────────────────────────

	private void removeRoomEmitter(Long roomId, SseEmitter emitter) {
		roomEmitters.compute(roomId, (id, list) -> {
			if (list == null) {
				return null;
			}
			list.remove(emitter);
			return list.isEmpty() ? null : list;
		});
	}

	private void removeUserEmitter(String userId, SseEmitter emitter) {
		userEmitters.compute(userId, (id, list) -> {
			if (list == null) {
				return null;
			}
			list.remove(emitter);
			return list.isEmpty() ? null : list;
		});
	}

	private void sendPing(SseEmitter emitter, String channelType, Object channelId) {
		try {
			emitter.send(SseEmitter.event().name("ping").data("connected"));
		} catch (IOException e) {
			log.debug("[SSE] initial ping failed - {}={}", channelType, channelId);
		}
	}
}
