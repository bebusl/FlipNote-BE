package flipnote.group.application.port.in;

import flipnote.group.application.port.in.command.CreateGroupCommand;
import flipnote.group.application.port.in.result.CreateGroupResult;

public interface CreateGroupUseCase {
	CreateGroupResult create(CreateGroupCommand cmd);
}
