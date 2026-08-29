package flipnote.group.application.port.in;

import flipnote.group.application.port.in.command.InviteRespondCommand;
import flipnote.group.application.port.in.result.InviteRespondResult;

public interface InviteRespondUseCase {
	InviteRespondResult respondToInvite(InviteRespondCommand cmd);
}
