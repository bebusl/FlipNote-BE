package flipnote.reaction.infrastructure.messaging;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import flipnote.reaction.domain.bookmark.event.BookmarkAddedEvent;
import flipnote.reaction.domain.bookmark.event.BookmarkRemovedEvent;
import flipnote.reaction.domain.like.event.LikeAddedEvent;
import flipnote.reaction.domain.like.event.LikeRemovedEvent;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ReactionEventListener {

	private final ReactionEventPublisher eventPublisher;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onBookmarkAdded(BookmarkAddedEvent event) {
		eventPublisher.bookmarkAdded(event.targetType(), event.targetId(), event.userId());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onBookmarkRemoved(BookmarkRemovedEvent event) {
		eventPublisher.bookmarkRemoved(event.targetType(), event.targetId(), event.userId());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onLikeAdded(LikeAddedEvent event) {
		eventPublisher.likeAdded(event.targetType(), event.targetId(), event.userId());
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onLikeRemoved(LikeRemovedEvent event) {
		eventPublisher.likeRemoved(event.targetType(), event.targetId(), event.userId());
	}
}
