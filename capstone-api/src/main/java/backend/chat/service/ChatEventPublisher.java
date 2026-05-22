package backend.chat.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import backend.chat.dto.ChatSseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 채팅 SSE 이벤트를 Redis Pub/Sub 채널로 발행한다.
 *
 * <p>채널 구조:
 * <ul>
 *   <li>{@code chat:room:{roomId}} — 방 단위 이벤트 (message / read / typing)</li>
 *   <li>{@code chat:user:{userId}} — 유저 단위 이벤트 (badge_update)</li>
 * </ul>
 *
 * <p>발행된 메시지는 모든 서버 인스턴스의 {@link ChatRedisSubscriber} 가 수신해
 * 로컬에 연결된 SSE 클라이언트에게 전달한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatEventPublisher {

	static final String ROOM_CHANNEL_PREFIX = "chat:room:";
	static final String USER_CHANNEL_PREFIX = "chat:user:";

	private final StringRedisTemplate stringRedisTemplate;
	private final ObjectMapper objectMapper;

	public void publishRoom(Long roomId, ChatSseEvent event) {
		publish(ROOM_CHANNEL_PREFIX + roomId, event);
	}

	public void publishUser(String userId, ChatSseEvent event) {
		publish(USER_CHANNEL_PREFIX + userId, event);
	}

	private void publish(String channel, ChatSseEvent event) {
		try {
			stringRedisTemplate.convertAndSend(channel, objectMapper.writeValueAsString(event));
		} catch (JsonProcessingException e) {
			log.error("[Redis] serialize failed - channel={}", channel, e);
		} catch (Exception e) {
			log.error("[Redis] publish failed - channel={}", channel, e);
		}
	}
}
