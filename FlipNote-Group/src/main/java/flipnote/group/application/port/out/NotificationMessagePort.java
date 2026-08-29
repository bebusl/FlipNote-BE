package flipnote.group.application.port.out;

import flipnote.group.application.dto.GroupInviteMessage;
import flipnote.group.application.dto.GroupJoinRequestMessage;

public interface NotificationMessagePort {
	void sendGroupInvite(GroupInviteMessage message);
	void sendGroupJoinRequest(GroupJoinRequestMessage message);
}
