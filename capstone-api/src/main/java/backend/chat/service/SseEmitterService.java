package backend.chat.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import backend.chat.dto.ChatMessageResponse;
import backend.chat.repository.ChatRoomRepository;
import backend.global.error.exception.BusinessException;
import backend.global.error.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SSE(Server-Sent Events) 구독·브로드캐스트를 담당하는 서비스.
 *
 * <p>채팅방 ID를 키로 Emitter 목록을 관리한다.
 * {@link CopyOnWriteArrayList} 로 emitter 순회 시 스레드 안전성을 보장하며,
 * removeEmitter 는 {@link Map#compute} 로 빈 리스트 제거를 원자적으로 처리한다.
 *
 * <p>현재는 단일 서버 인메모리 방식이다.
 * 스케일아웃 환경에서는 Redis Pub/Sub 으로 교체할 것을 권장한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SseEmitterService {

	/** SSE 연결 유지 시간: 10분 */
	private static final long SSE_TIMEOUT_MS = 10 * 60 * 1000L;

	/** roomId → 해당 방을 구독 중인 Emitter 목록 */
	private final Map<Long, List<SseEmitter>> roomEmitters = new ConcurrentHashMap<>();

	private final ChatRoomRepository chatRoomRepository;

	/**
	 * 특정 채팅방에 SSE 구독을 등록하고 Emitter를 반환합니다.
	 * 존재하지 않는 채팅방 구독을 방지합니다.
	 *
	 * @param roomId 구독할 채팅방 ID
	 * @return 생성된 SseEmitter
	 */
	public SseEmitter subscribe(Long roomId) {
		chatRoomRepository.findById(roomId) // 채팅방 존재 검증
			.orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

		SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);

		roomEmitters.computeIfAbsent(roomId, id -> new CopyOnWriteArrayList<>()).add(emitter);

		emitter.onCompletion(() -> removeEmitter(roomId, emitter));
		emitter.onTimeout(() -> {
			log.debug("[SSE] timeout - roomId={}", roomId);
			removeEmitter(roomId, emitter);
		});
		emitter.onError(e -> {
			log.debug("[SSE] error - roomId={}, error={}", roomId, e.getMessage());
			removeEmitter(roomId, emitter);
		});

		sendPing(emitter, roomId);

		log.debug("[SSE] subscribed - roomId={}", roomId);
		return emitter;
	}

	/**
	 * 채팅방에 새 메시지를 구독 중인 모든 클라이언트에게 브로드캐스트합니다.
	 *
	 * @param roomId  대상 채팅방 ID
	 * @param message 전송할 메시지 DTO
	 */
	public void broadcast(Long roomId, ChatMessageResponse message) {
		List<SseEmitter> emitters = roomEmitters.getOrDefault(roomId, List.of());

		if (emitters.isEmpty()) {
			return;
		}

		SseEmitter.SseEventBuilder event = SseEmitter.event()
			.name("message")
			.data(message);

		for (SseEmitter emitter : emitters) {
			try {
				emitter.send(event);
			} catch (IOException e) {
				log.debug("[SSE] send failed - roomId={}, removing emitter", roomId);
				removeEmitter(roomId, emitter);
			}
		}
	}

	/**
	 * 특정 Emitter를 방 구독 목록에서 원자적으로 제거합니다.
	 *
	 * <p>Map.compute 를 사용해 "빈 리스트 감지 → 맵 항목 제거" 를 단일 락 범위 내에서 처리,
	 * 신규 구독자가 방금 추가한 emitter 가 경쟁적으로 삭제되는 race condition 을 방지합니다.
	 */
	private void removeEmitter(Long roomId, SseEmitter emitter) {
		roomEmitters.compute(roomId, (id, list) -> {
			if (list == null) {
				return null;
			}
			list.remove(emitter);
			return list.isEmpty() ? null : list;
		});
	}

	/**
	 * 연결 직후 핑 이벤트를 전송합니다.
	 * 일부 브라우저/프록시는 첫 데이터가 없으면 연결을 닫아 버리므로 필요합니다.
	 */
	private void sendPing(SseEmitter emitter, Long roomId) {
		try {
			emitter.send(SseEmitter.event().name("ping").data("connected"));
		} catch (IOException e) {
			log.debug("[SSE] initial ping failed - roomId={}", roomId);
		}
	}
}
