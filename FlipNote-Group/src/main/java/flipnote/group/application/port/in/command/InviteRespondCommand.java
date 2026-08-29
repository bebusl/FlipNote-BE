package flipnote.group.application.port.in.command;

import flipnote.group.domain.model.invite.InviteStatus;

public record InviteRespondCommand(
	Long groupId,
	Long inviteeUserId,
	Long invitationId,
	InviteStatus status
) {
}
