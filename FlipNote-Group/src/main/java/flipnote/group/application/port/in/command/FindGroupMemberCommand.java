package flipnote.group.application.port.in.command;

public record FindGroupMemberCommand(
	Long userId,
	Long groupId
) {
}
