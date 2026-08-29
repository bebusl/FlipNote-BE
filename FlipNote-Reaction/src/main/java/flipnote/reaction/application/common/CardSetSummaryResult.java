package flipnote.reaction.application.common;

public record CardSetSummaryResult(
	Long id,
	String name,
	Long groupId,
	String visibility,
	String category,
	String hashtag,
	Long imageRefId,
	Long cardCount
) {
}
