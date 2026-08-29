package flipnote.group.application.port.in.result;

import java.util.List;

import flipnote.group.domain.model.member.GroupMemberRole;
import flipnote.group.domain.model.permission.GroupPermission;

public record AddPermissionResult(
	List<GroupPermission> groupPermissions,
	GroupMemberRole role
) {
	public static AddPermissionResult of(List<GroupPermission> groupPermissions, GroupMemberRole role) {
		return new AddPermissionResult(groupPermissions, role);
	}
}
