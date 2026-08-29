package flipnote.reaction.domain.bookmark.event;

public record BookmarkAddedEvent(
	String targetType,
	Long targetId,
	Long userId
) {
}
