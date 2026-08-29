package flipnote.group.application.port.in.command;

public record KickMemberCommand(
	Long userId,
	Long groupId,
	Long memberId
) {
}
