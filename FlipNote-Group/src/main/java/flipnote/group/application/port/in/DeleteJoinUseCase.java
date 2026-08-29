package flipnote.group.application.port.in;

import flipnote.group.application.port.in.command.DeleteJoinCommand;

public interface DeleteJoinUseCase {
	void deleteJoin(DeleteJoinCommand cmd);
}
