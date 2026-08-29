package flipnote.group.domain.model.invite;

import java.time.LocalDateTime;

import flipnote.group.adapter.out.entity.InviteEntity;

public record InviteInfo(
	Long invitationId,
	Long inviterUserId,
	Long inviteeUserId,
	String inviteeEmail,
	String inviteeNickname,
	InviteStatus status,
	LocalDateTime createdAt
) {
	public static InviteInfo of(InviteEntity invite, String inviteeNickname) {
		return new InviteInfo(
			invite.getId(),
			invite.getInviterUserId(),
			invite.getInviteeUserId(),
			invite.getInviteeEmail(),
			inviteeNickname,
			invite.getStatus(),
			invite.getCreatedAt()
		);
	}
}
