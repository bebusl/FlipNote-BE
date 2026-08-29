package flipnote.notification.infrastructure.messaging;

import java.util.List;

public record GroupJoinRequestMessage(
	Long groupId,
	List<Long> receiverIds,
	Long requesterId,
	String requesterNickname
) {
}
