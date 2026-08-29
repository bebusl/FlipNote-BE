package flipnote.notification.domain.notification;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
	GROUP_INVITE("notification.group.invite"),
	GROUP_JOIN_REQUEST("notification.group.join.request");

	private final String messageKey;
}
