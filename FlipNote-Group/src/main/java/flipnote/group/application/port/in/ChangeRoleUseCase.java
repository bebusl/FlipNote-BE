package flipnote.group.application.port.in;

import flipnote.group.application.port.in.command.ChangeRoleCommand;
import flipnote.group.application.port.in.result.ChangeRoleResult;

public interface ChangeRoleUseCase {
	ChangeRoleResult ChangeRole(ChangeRoleCommand cmd);
}
