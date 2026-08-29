package flipnote.notification.application.dto.result;

import java.time.LocalDateTime;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

import flipnote.notification.domain.notification.Notification;

public record NotificationResult(
	Long notificationId,
	Long groupId,
	String message,
	Map<String, Object> metadata,
	boolean isRead,
	LocalDateTime readAt,
	LocalDateTime createdAt
) {

	public static NotificationResult of(Notification notification, String message) {
		return new NotificationResult(
			notification.getId(),
			notification.getGroupId(),
			message,
			notification.getMetadata(),
			notification.isRead(),
			notification.getReadAt(),
			notification.getCreatedAt()
		);
	}
}
