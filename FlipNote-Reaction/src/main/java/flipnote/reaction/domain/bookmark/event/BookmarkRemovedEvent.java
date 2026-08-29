package flipnote.reaction.domain.bookmark.event;

public record BookmarkRemovedEvent(
	String targetType,
	Long targetId,
	Long userId
) {
}
