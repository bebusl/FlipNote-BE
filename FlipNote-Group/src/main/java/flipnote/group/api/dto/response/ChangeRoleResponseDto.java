package flipnote.group.api.dto.response;

import flipnote.group.application.port.in.result.ChangeRoleResult;
import flipnote.group.domain.model.member.GroupMemberRole;

public record ChangeRoleResponseDto(
	Long memberId,
	GroupMemberRole role
) {
	public static ChangeRoleResponseDto of(ChangeRoleResult result) {
		return new ChangeRoleResponseDto(result.memberId(), result.role());
	}
}
