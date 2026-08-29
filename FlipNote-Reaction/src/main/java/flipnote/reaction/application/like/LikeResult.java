package flipnote.reaction.application.like;

import java.time.LocalDateTime;

import flipnote.reaction.application.common.CardSetSummaryResult;
import flipnote.reaction.domain.like.Like;

public record LikeResult(
	String targetType,
	Long targetId,
	LocalDateTime likedAt,
	CardSetSummaryResult cardSet
) {
	public static LikeResult from(Like like, CardSetSummaryResult cardSet) {
		return new LikeResult(
			like.getTargetType().name(),
			like.getTargetId(),
			like.getCreatedAt(),
			cardSet
		);
	}
}
