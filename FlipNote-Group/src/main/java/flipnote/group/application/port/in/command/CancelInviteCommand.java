package flipnote.group.application.port.in.command;

public record CancelInviteCommand(
	Long userId,
	Long groupId,
	Long invitationId
) {
}
