package flipnote.notification.infrastructure.messaging;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import flipnote.notification.application.NotificationCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class GroupInviteMessageListener {

	private final NotificationCommandService notificationCommandService;

	@RabbitListener(queues = RabbitMQConfig.GROUP_INVITE_QUEUE)
	public void handleGroupInvite(GroupInviteMessage message) {
		log.info("Received group invite message: groupId={}, inviteeId={}", message.groupId(), message.inviteeId());
		notificationCommandService.createGroupInviteNotification(
			message.groupId(),
			message.inviteeId(),
			message.groupName()
		);
	}
}
