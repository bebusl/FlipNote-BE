package flipnote.notification.application.dto.command;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public record NotificationListCommand(
	Long cursorId,
	Long groupId,
	Boolean read,
	int size
) {

	public PageRequest getPageRequest() {
		return PageRequest.of(0, size + 1, Sort.by(Sort.Direction.DESC, "id"));
	}
}
