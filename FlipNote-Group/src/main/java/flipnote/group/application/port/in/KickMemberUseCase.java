package flipnote.group.application.port.in;

import flipnote.group.application.port.in.command.KickMemberCommand;

public interface KickMemberUseCase {
	void kickMember(KickMemberCommand cmd);
}
