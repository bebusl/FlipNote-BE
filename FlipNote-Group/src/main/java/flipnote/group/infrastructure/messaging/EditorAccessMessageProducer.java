package flipnote.group.infrastructure.messaging;

import java.util.Map;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class EditorAccessMessageProducer {

	private final RabbitTemplate rabbitTemplate;

	public void sendGroupMemberKicked(Long groupId, Long userId) {
		rabbitTemplate.convertAndSend(
			RabbitMQConfig.EDITOR_ACCESS_EXCHANGE,
			RabbitMQConfig.GROUP_MEMBER_KICKED_ROUTING_KEY,
			Map.of(
				"eventType", "GROUP_MEMBER_KICKED",
				"groupId", groupId,
				"userId", userId,
				"reason", "GROUP_MEMBER_KICKED"
			)
		);
	}
}
