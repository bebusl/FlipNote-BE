package flipnote.group.infrastructure.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import flipnote.group.application.dto.GroupInviteMessage;
import flipnote.group.application.dto.GroupJoinRequestMessage;
import flipnote.group.application.port.out.NotificationMessagePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class GroupJoinRequestMessageProducer implements NotificationMessagePort {

	private final RabbitTemplate rabbitTemplate;

	@Override
	public void sendGroupInvite(GroupInviteMessage message) {
		log.info("GroupInviteMessage publish: groupId={}, inviteeId={}", message.groupId(), message.inviteeId());
		rabbitTemplate.convertAndSend(
			RabbitMQConfig.EXCHANGE,
			RabbitMQConfig.GROUP_INVITE_ROUTING_KEY,
			message
		);
	}

	@Override
	public void sendGroupJoinRequest(GroupJoinRequestMessage message) {
		log.info("GroupJoinRequestMessage publish: groupId={}, requesterId={}", message.groupId(), message.requesterId());
		rabbitTemplate.convertAndSend(
			RabbitMQConfig.EXCHANGE,
			RabbitMQConfig.GROUP_JOIN_REQUEST_ROUTING_KEY,
			message
		);
	}
}
