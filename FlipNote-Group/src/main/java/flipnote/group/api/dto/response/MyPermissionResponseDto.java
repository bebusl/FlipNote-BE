package flipnote.group.api.dto.response;

import java.util.List;

import flipnote.group.application.port.in.result.MyPermissionResult;
import flipnote.group.domain.model.member.GroupMemberRole;
import flipnote.group.domain.model.permission.GroupPermission;

public record MyPermissionResponseDto(
	GroupMemberRole role,
	List<GroupPermission> permissions
) {
	public static MyPermissionResponseDto from(MyPermissionResult result) {
		return new MyPermissionResponseDto(result.role(), result.permissions());
	}
}
