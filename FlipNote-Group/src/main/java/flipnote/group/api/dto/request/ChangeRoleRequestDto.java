package flipnote.group.api.dto.request;

import flipnote.group.domain.model.member.GroupMemberRole;
import jakarta.validation.constraints.NotNull;

public record ChangeRoleRequestDto(
	@NotNull GroupMemberRole role
) {
}
