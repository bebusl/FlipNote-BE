package flipnote.group.application.port.in.result;

import java.util.List;

import flipnote.group.domain.model.member.GroupMemberRole;
import flipnote.group.domain.model.permission.GroupPermission;

public record MyPermissionResult(
	GroupMemberRole role,
	List<GroupPermission> permissions
) {
	public static MyPermissionResult of(GroupMemberRole role, List<GroupPermission> permissions) {
		return new MyPermissionResult(role, permissions);
	}
}
