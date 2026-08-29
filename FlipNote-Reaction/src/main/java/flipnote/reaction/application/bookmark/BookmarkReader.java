package flipnote.reaction.application.bookmark;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import flipnote.reaction.domain.bookmark.Bookmark;
import flipnote.reaction.domain.bookmark.BookmarkErrorCode;
import flipnote.reaction.domain.bookmark.BookmarkRepository;
import flipnote.reaction.domain.bookmark.BookmarkTargetType;
import flipnote.reaction.domain.common.BizException;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BookmarkReader {

	private final BookmarkRepository bookmarkRepository;

	public Bookmark findByTargetAndUserId(BookmarkTargetType targetType, Long targetId, Long userId) {
		return bookmarkRepository.findByTargetTypeAndTargetIdAndUserId(targetType, targetId, userId)
			.orElseThrow(() -> new BizException(BookmarkErrorCode.BOOKMARK_NOT_FOUND));
	}

	public boolean isBookmarked(BookmarkTargetType targetType, Long targetId, Long userId) {
		return bookmarkRepository.existsByTargetTypeAndTargetIdAndUserId(targetType, targetId, userId);
	}

	public Map<Long, Boolean> areBookmarked(BookmarkTargetType targetType, List<Long> targetIds, long userId) {
		return targetIds.stream()
			.collect(Collectors.toMap(
				targetId -> targetId,
				targetId -> bookmarkRepository.existsByTargetTypeAndTargetIdAndUserId(targetType, targetId, userId)
			));
	}
}
