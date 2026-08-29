package flipnote.group.domain.event;

public record GuestInviteCreatedEvent(
	String email,
	String groupName
) {
}
