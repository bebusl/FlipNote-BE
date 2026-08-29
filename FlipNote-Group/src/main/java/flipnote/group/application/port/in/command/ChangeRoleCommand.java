package flipnote.group.application.port.in.command;

import flipnote.group.domain.model.member.GroupMemberRole;

public record ChangeRoleCommand(
	Long userId,
	Long memberId,
	Long groupId,
	GroupMemberRole changeRole
) {
}
