package flipnote.group.application.port.in.result;

import flipnote.group.adapter.out.entity.GroupMemberEntity;
import flipnote.group.domain.model.member.GroupMemberRole;

public record ChangeRoleResult(
	Long memberId,
	GroupMemberRole role
) {

	public static ChangeRoleResult of(GroupMemberEntity groupMember) {
		return new ChangeRoleResult(groupMember.getId(), groupMember.getRole().getRole());
	}
}
