package flipnote.notification.infrastructure.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import flipnote.notification.application.NotificationCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class GroupJoinRequestMessageListener {

	private final NotificationCommandService notificationCommandService;

	@RabbitListener(queues = RabbitMQConfig.GROUP_JOIN_REQUEST_QUEUE)
	public void handleGroupJoinRequest(GroupJoinRequestMessage message) {
		log.info("Received group join request message: groupId={}, requesterId={}",
			message.groupId(), message.requesterId());
		notificationCommandService.createGroupJoinRequestNotification(
			message.groupId(),
			message.receiverIds(),
			message.requesterId(),
			message.requesterNickname()
		);
	}
}
