package flipnote.group.application.port.in.command;

public record ApplicationFormCommand(
	Long userId,
	Long groupId,
	String joinIntro
) {
}
