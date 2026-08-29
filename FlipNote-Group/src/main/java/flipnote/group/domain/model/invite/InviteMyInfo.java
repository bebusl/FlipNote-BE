package flipnote.group.domain.model.invite;

import java.time.LocalDateTime;

import flipnote.group.adapter.out.entity.InviteEntity;

public record InviteMyInfo(
	Long invitationId,
	Long groupId,
	InviteStatus status,
	LocalDateTime createdAt
) {
	public static InviteMyInfo of(InviteEntity invite) {
		return new InviteMyInfo(
			invite.getId(),
			invite.getGroupId(),
			invite.getStatus(),
			invite.getCreatedAt()
		);
	}
}
