package backend.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import backend.chat.service.ChatRedisSubscriber;
import lombok.RequiredArgsConstructor;

/**
 * 채팅 Redis Pub/Sub 설정.
 * {@code chat:*} 패턴의 모든 채널을 {@link ChatRedisSubscriber} 가 구독한다.
 */
@Configuration
@RequiredArgsConstructor
public class ChatRedisConfig {

	private final ChatRedisSubscriber chatRedisSubscriber;

	@Bean
	public RedisMessageListenerContainer chatRedisMessageListenerContainer(
			RedisConnectionFactory connectionFactory) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.addMessageListener(chatRedisSubscriber, new PatternTopic("chat:*"));
		return container;
	}
}
