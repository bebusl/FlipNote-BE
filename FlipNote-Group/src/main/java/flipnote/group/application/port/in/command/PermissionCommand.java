package flipnote.group.application.port.in.command;

import flipnote.group.domain.model.member.GroupMemberRole;
import flipnote.group.domain.model.permission.GroupPermission;

public record PermissionCommand(
	Long userId,
	Long groupId,
	GroupMemberRole changeRole,
	GroupPermission permission) {

}
