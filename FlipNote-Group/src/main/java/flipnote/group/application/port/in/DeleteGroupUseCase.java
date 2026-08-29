package flipnote.group.application.port.in;

import flipnote.group.application.port.in.command.DeleteGroupCommand;

public interface DeleteGroupUseCase {
	void deleteGroup(DeleteGroupCommand cmd);
}
