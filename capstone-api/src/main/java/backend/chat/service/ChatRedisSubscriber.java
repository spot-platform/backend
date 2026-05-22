package backend.chat.service;

import java.nio.charset.StandardCharsets;

import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import backend.chat.dto.ChatSseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis Pub/Sub 구독자. 모든 {@code chat:*} 채널 메시지를 수신해
 * 로컬 {@link SseEmitterService} 에 전달한다.
 *
 * <p>스케일아웃 환경에서는 각 인스턴스가 이 클래스를 실행하므로,
 * 어느 인스턴스가 메시지를 발행하더라도 모든 인스턴스의 SSE 클라이언트가 이벤트를 수신한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRedisSubscriber implements MessageListener {

	private final SseEmitterService sseEmitterService;
	private final ObjectMapper objectMapper;

	@Override
	public void onMessage(Message message, byte[] pattern) {
		String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
		try {
			ChatSseEvent event = objectMapper.readValue(message.getBody(), ChatSseEvent.class);
			if (channel.startsWith(ChatEventPublisher.ROOM_CHANNEL_PREFIX)) {
				Long roomId = Long.parseLong(channel.substring(ChatEventPublisher.ROOM_CHANNEL_PREFIX.length()));
				sseEmitterService.sendRoomEventLocal(roomId, event);
			} else if (channel.startsWith(ChatEventPublisher.USER_CHANNEL_PREFIX)) {
				String userId = channel.substring(ChatEventPublisher.USER_CHANNEL_PREFIX.length());
				sseEmitterService.sendUserEventLocal(userId, event);
			}
		} catch (Exception e) {
			log.error("[Redis] subscriber error - channel={}", channel, e);
		}
	}
}
