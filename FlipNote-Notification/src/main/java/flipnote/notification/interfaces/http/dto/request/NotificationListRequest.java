package flipnote.notification.interfaces.http.dto.request;

import flipnote.notification.application.dto.command.NotificationListCommand;
import flipnote.notification.interfaces.http.common.CursorPagingRequest;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NotificationListRequest extends CursorPagingRequest {

	@Min(1)
	private Long groupId;

	private Boolean read;

	public NotificationListCommand toCommand() {
		return new NotificationListCommand(getCursorId(), groupId, read, getSize());
	}
}
