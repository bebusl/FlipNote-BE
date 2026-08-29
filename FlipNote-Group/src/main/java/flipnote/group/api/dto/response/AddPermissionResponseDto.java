package flipnote.group.api.dto.response;

import java.util.List;

import flipnote.group.application.port.in.result.AddPermissionResult;
import flipnote.group.domain.model.member.GroupMemberRole;
import flipnote.group.domain.model.permission.GroupPermission;

public record AddPermissionResponseDto(
	GroupMemberRole role,
	List<GroupPermission> permissions
) {

	public static AddPermissionResponseDto from(AddPermissionResult result) {
		return new AddPermissionResponseDto(result.role(), result.groupPermissions());
	}
}
