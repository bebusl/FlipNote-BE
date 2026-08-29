package flipnote.reaction.domain.like.event;

public record LikeAddedEvent(
	String targetType,
	Long targetId,
	Long userId
) {
}
