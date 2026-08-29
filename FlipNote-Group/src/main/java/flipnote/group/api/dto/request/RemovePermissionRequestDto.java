package flipnote.group.api.dto.request;

import flipnote.group.domain.model.member.GroupMemberRole;
import flipnote.group.domain.model.permission.GroupPermission;

public record RemovePermissionRequestDto(
	GroupMemberRole changeRole,
	GroupPermission permission
) {
}
