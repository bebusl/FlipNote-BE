package flipnote.reaction.domain.like.event;

public record LikeRemovedEvent(
	String targetType,
	Long targetId,
	Long userId
) {
}
