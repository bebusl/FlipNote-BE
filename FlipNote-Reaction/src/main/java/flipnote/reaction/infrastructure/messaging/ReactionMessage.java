package flipnote.reaction.infrastructure.messaging;

public record ReactionMessage(
	String eventType,
	String targetType,
	Long targetId,
	Long userId
) {
}
