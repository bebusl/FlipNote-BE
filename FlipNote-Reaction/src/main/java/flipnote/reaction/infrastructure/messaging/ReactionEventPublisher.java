package flipnote.reaction.infrastructure.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReactionEventPublisher {

	private final RabbitTemplate rabbitTemplate;

	public void bookmarkAdded(String targetType, Long targetId, Long userId) {
		publish(RabbitMqConfig.ROUTING_KEY_BOOKMARK_ADDED, "BOOKMARK_ADDED", targetType, targetId, userId);
	}

	public void bookmarkRemoved(String targetType, Long targetId, Long userId) {
		publish(RabbitMqConfig.ROUTING_KEY_BOOKMARK_REMOVED, "BOOKMARK_REMOVED", targetType, targetId, userId);
	}

	public void likeAdded(String targetType, Long targetId, Long userId) {
		publish(RabbitMqConfig.ROUTING_KEY_LIKE_ADDED, "LIKE_ADDED", targetType, targetId, userId);
	}

	public void likeRemoved(String targetType, Long targetId, Long userId) {
		publish(RabbitMqConfig.ROUTING_KEY_LIKE_REMOVED, "LIKE_REMOVED", targetType, targetId, userId);
	}

	private void publish(String routingKey, String eventType, String targetType, Long targetId, Long userId) {
		try {
			rabbitTemplate.convertAndSend(
				RabbitMqConfig.EXCHANGE,
				routingKey,
				new ReactionMessage(eventType, targetType, targetId, userId)
			);
		} catch (Exception e) {
			log.error("이벤트 발행 실패: eventType={}, targetType={}, targetId={}, userId={}",
				eventType, targetType, targetId, userId, e);
		}
	}
}
