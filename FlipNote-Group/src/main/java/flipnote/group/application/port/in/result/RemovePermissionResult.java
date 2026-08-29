package flipnote.group.application.port.in.result;

import java.util.List;

import flipnote.group.domain.model.member.GroupMemberRole;
import flipnote.group.domain.model.permission.GroupPermission;

public record RemovePermissionResult(
	List<GroupPermission> groupPermissions,
	GroupMemberRole role
) {
	public static RemovePermissionResult of(List<GroupPermission> groupPermissions, GroupMemberRole role) {
		return new RemovePermissionResult(groupPermissions, role);
	}
}
