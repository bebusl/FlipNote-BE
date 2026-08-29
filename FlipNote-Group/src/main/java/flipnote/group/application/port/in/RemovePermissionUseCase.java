package flipnote.group.application.port.in;

import flipnote.group.application.port.in.command.PermissionCommand;
import flipnote.group.application.port.in.result.RemovePermissionResult;

public interface RemovePermissionUseCase {
	RemovePermissionResult removePermission(PermissionCommand cmd);
}
