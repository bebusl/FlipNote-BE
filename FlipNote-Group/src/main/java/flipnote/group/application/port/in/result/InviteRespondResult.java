package flipnote.group.application.port.in.result;

import flipnote.group.adapter.out.entity.InviteEntity;

public record InviteRespondResult(
	InviteEntity invite
) {
}
