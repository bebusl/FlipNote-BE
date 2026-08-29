package flipnote.group.application.dto;

public record GroupInviteMessage(
	Long groupId,
	Long inviteeId,
	String groupName
) {
}
