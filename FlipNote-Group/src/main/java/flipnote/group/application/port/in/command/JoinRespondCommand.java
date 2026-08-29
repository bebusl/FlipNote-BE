package flipnote.group.application.port.in.command;

import flipnote.group.domain.model.join.JoinStatus;

public record JoinRespondCommand(
	Long groupId,
	Long userId,
	Long joinId,
	JoinStatus status
) {
}
