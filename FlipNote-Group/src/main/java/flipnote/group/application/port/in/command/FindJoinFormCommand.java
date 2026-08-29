package flipnote.group.application.port.in.command;

public record FindJoinFormCommand(
	Long userId,
	Long groupId
) {
}
