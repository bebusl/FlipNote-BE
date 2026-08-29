package flipnote.reaction.application.bookmark;

import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.reaction.application.common.CardSetSummaryResult;
import flipnote.reaction.domain.bookmark.Bookmark;
import flipnote.reaction.domain.bookmark.BookmarkErrorCode;
import flipnote.reaction.domain.bookmark.BookmarkRepository;
import flipnote.reaction.domain.bookmark.BookmarkTargetType;
import flipnote.reaction.domain.bookmark.event.BookmarkAddedEvent;
import flipnote.reaction.domain.bookmark.event.BookmarkRemovedEvent;
import flipnote.reaction.domain.common.BizException;
import flipnote.reaction.domain.common.CommonErrorCode;
import flipnote.reaction.infrastructure.grpc.CardSetGrpcClient;
import flipnote.reaction.interfaces.http.dto.request.BookmarkSearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

	private final BookmarkRepository bookmarkRepository;
	private final BookmarkReader bookmarkReader;
	private final ApplicationEventPublisher eventPublisher;
	private final CardSetGrpcClient cardSetGrpcClient;

	@Transactional(noRollbackFor = DataIntegrityViolationException.class)
	public Long addBookmark(BookmarkTargetType targetType, Long targetId, Long userId) {
		if (!cardSetGrpcClient.isCardSetViewable(targetId, userId)) {
			throw new BizException(CommonErrorCode.TARGET_NOT_VIEWABLE);
		}

		if (bookmarkReader.isBookmarked(targetType, targetId, userId)) {
			throw new BizException(BookmarkErrorCode.ALREADY_BOOKMARKED);
		}

		Bookmark bookmark = Bookmark.builder()
			.targetType(targetType)
			.targetId(targetId)
			.userId(userId)
			.build();

		try {
			bookmarkRepository.save(bookmark);
		} catch (DataIntegrityViolationException e) {
			throw new BizException(BookmarkErrorCode.ALREADY_BOOKMARKED);
		}

		eventPublisher.publishEvent(new BookmarkAddedEvent(targetType.name(), targetId, userId));

		return bookmark.getId();
	}

	@Transactional
	public void removeBookmark(BookmarkTargetType targetType, Long targetId, Long userId) {
		Bookmark bookmark = bookmarkReader.findByTargetAndUserId(targetType, targetId, userId);
		bookmarkRepository.delete(bookmark);

		eventPublisher.publishEvent(new BookmarkRemovedEvent(targetType.name(), targetId, userId));
	}

	public Page<BookmarkResult> getBookmarks(BookmarkTargetType targetType, Long userId,
		BookmarkSearchRequest request) {
		Page<Bookmark> bookmarkPage = bookmarkRepository.findByTargetTypeAndUserId(
			targetType, userId, request.getPageRequest()
		);

		List<Long> targetIds = bookmarkPage.getContent().stream()
			.map(Bookmark::getTargetId)
			.toList();

		Map<Long, CardSetSummaryResult> cardSetMap = fetchCardSets(targetIds, userId);

		return bookmarkPage.map(b -> BookmarkResult.from(b, cardSetMap.get(b.getTargetId())));
	}

	private Map<Long, CardSetSummaryResult> fetchCardSets(List<Long> targetIds, Long userId) {
		if (targetIds.isEmpty()) {
			return Map.of();
		}
		try {
			return cardSetGrpcClient.getCardSetsByIds(targetIds, userId);
		} catch (BizException e) {
			log.warn("CardSet 조회 실패 — cardSet 없이 반환합니다: {}", e.getMessage());
			return Map.of();
		}
	}
}
