package flipnote.group.domain.model.invite;

public enum InviteResponseStatus {
	ACCEPTED, REJECTED;

	public InviteStatus toInviteStatus() {
		return InviteStatus.valueOf(this.name());
	}
}
