package flipnote.notification.application;

import java.util.List;
import java.util.Locale;

import org.apache.commons.text.StringSubstitutor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.notification.application.dto.command.NotificationListCommand;
import flipnote.notification.application.dto.result.NotificationResult;
import flipnote.notification.application.dto.result.PagedResult;
import flipnote.notification.domain.notification.Notification;
import flipnote.notification.domain.notification.NotificationRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class NotificationQueryService {

	private final NotificationRepository notificationRepository;
	private final MessageSource messageSource;

	public PagedResult<NotificationResult> getNotifications(Long userId, NotificationListCommand command) {
		List<Notification> notifications = notificationRepository.findNotificationsByReceiverIdAndCursor(
			userId, command.cursorId(), command.groupId(), command.read(), command.getPageRequest()
		);

		boolean hasNext = notifications.size() > command.size();
		Long nextCursor = null;
		if (hasNext) {
			notifications = notifications.subList(0, command.size());
			nextCursor = notifications.get(notifications.size() - 1).getId();
		}

		List<NotificationResult> content = notifications.stream()
			.map(notification -> NotificationResult.of(notification, buildMessage(notification)))
			.toList();

		return PagedResult.of(content, hasNext, nextCursor);
	}

	private String buildMessage(Notification notification) {
		String key = notification.getType().getMessageKey();
		String template = messageSource.getMessage(key, null, key, Locale.KOREA);
		StringSubstitutor substitutor = new StringSubstitutor(notification.getVariables());
		return substitutor.replace(template);
	}
}
