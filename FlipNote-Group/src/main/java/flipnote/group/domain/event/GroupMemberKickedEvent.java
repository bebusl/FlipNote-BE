package flipnote.group.domain.event;

public record GroupMemberKickedEvent(
	Long groupId,
	Long userId
) {
}
