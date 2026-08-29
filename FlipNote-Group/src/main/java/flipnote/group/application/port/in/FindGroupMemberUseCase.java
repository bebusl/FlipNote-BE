package flipnote.group.application.port.in;

import flipnote.group.application.port.in.command.FindGroupMemberCommand;
import flipnote.group.application.port.in.result.FindGroupMemberResult;

public interface FindGroupMemberUseCase {
	FindGroupMemberResult findGroupMember(FindGroupMemberCommand cmd);
}
