package flipnote.group.application.port.in.command;

public record CreateInviteCommand(
	Long inviterUserId,
	String inviterEmail,
	Long groupId,
	String inviteeEmail
) {
}
