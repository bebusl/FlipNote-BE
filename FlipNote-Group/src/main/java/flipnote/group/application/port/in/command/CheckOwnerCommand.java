package flipnote.group.application.port.in.command;

public record CheckOwnerCommand(
	Long userId,
	Long groupId
) {
}
