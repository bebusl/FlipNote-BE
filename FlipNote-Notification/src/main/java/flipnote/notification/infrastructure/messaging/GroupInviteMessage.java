package flipnote.notification.infrastructure.messaging;

public record GroupInviteMessage(
	Long groupId,
	Long inviteeId,
	String groupName
) {
}
