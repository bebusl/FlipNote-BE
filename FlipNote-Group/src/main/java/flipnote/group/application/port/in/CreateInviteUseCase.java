package flipnote.group.application.port.in;

import flipnote.group.application.port.in.command.CreateInviteCommand;
import flipnote.group.application.port.in.result.CreateInviteResult;

public interface CreateInviteUseCase {
	CreateInviteResult createInvite(CreateInviteCommand cmd);
}
