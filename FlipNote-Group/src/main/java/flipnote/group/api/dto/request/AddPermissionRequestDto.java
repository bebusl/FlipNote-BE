package flipnote.group.api.dto.request;

import flipnote.group.domain.model.member.GroupMemberRole;
import flipnote.group.domain.model.permission.GroupPermission;

public record AddPermissionRequestDto(
	GroupMemberRole changeRole,
	GroupPermission permission
) {
}
