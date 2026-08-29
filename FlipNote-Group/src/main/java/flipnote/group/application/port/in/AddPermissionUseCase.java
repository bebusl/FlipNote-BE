package flipnote.group.application.port.in;

import flipnote.group.application.port.in.command.PermissionCommand;
import flipnote.group.application.port.in.result.AddPermissionResult;

public interface AddPermissionUseCase {
	AddPermissionResult addPermission(PermissionCommand cmd);
}
