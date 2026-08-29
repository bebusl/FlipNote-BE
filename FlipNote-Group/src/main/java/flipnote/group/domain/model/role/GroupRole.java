package flipnote.group.domain.model.role;

import flipnote.group.domain.model.member.GroupMemberRole;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GroupRole {
	private Long id;
	private Long groupId;
	private GroupMemberRole role;

	@Builder
	private GroupRole(Long id, Long groupId, GroupMemberRole role) {
		this.id = id;
		this.groupId = groupId;
		this.role = role;
	}
}
