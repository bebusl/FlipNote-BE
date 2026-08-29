package flipnote.group.application.port.in.command;

public record FindGroupCommand(
	Long userId,
	Long groupId
) {
}
