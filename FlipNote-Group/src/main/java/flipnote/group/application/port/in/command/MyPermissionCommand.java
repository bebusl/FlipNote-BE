package flipnote.group.application.port.in.command;

public record MyPermissionCommand(
	Long groupId,
	Long userId
) {
}
