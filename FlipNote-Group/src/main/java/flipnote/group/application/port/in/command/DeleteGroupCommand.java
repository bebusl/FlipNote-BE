package flipnote.group.application.port.in.command;

public record DeleteGroupCommand(
	Long userId,
	Long groupId
) {
}
