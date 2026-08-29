package flipnote.group.application.port.in;

import flipnote.group.application.port.in.command.CancelInviteCommand;

public interface CancelInviteUseCase {
	void cancelInvite(CancelInviteCommand cmd);
}
