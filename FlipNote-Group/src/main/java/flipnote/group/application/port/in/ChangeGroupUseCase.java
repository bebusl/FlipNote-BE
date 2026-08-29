package flipnote.group.application.port.in;

import flipnote.group.application.port.in.command.ChangeGroupCommand;
import flipnote.group.application.port.in.result.ChangeGroupResult;

public interface ChangeGroupUseCase {
	ChangeGroupResult change(ChangeGroupCommand cmd);
}
