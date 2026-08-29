package flipnote.group.api.dto.response;

import java.util.List;

import flipnote.group.application.port.in.result.RemovePermissionResult;
import flipnote.group.domain.model.member.GroupMemberRole;
import flipnote.group.domain.model.permission.GroupPermission;

public record RemovePermissionResponseDto(
	GroupMemberRole role,
	List<GroupPermission> permissions
) {
	public static RemovePermissionResponseDto from(RemovePermissionResult result) {
		return new RemovePermissionResponseDto(result.role(), result.groupPermissions());
	}
}
