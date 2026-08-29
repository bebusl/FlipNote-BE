package flipnote.group.application.port.in;

import flipnote.group.application.port.in.command.CheckOwnerCommand;
import flipnote.group.application.port.in.result.CheckOwnerResult;

public interface CheckOwnerUseCase {
	CheckOwnerResult checkOwner(CheckOwnerCommand cmd);
}
