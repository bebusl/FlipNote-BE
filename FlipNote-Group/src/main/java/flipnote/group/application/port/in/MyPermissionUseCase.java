package flipnote.group.application.port.in;

import flipnote.group.application.port.in.command.MyPermissionCommand;
import flipnote.group.application.port.in.result.MyPermissionResult;

public interface MyPermissionUseCase {
	MyPermissionResult findMyPermission(MyPermissionCommand cmd);
}
