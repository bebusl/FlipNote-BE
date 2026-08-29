package flipnote.group.application.port.in.command;

public record DeleteJoinCommand(
	Long userId,
	Long joinId
) {
}
