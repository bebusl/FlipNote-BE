package flipnote.reaction.interfaces.http.dto.request;

import flipnote.reaction.domain.bookmark.BookmarkErrorCode;
import flipnote.reaction.domain.bookmark.BookmarkTargetType;
import flipnote.reaction.domain.common.BizException;

public enum BookmarkTargetTypeRequest {
	card_set;

	public BookmarkTargetType toEntity() {
		return switch (this) {
			case card_set -> BookmarkTargetType.CARD_SET;
		};
	}

	public static BookmarkTargetTypeRequest from(String value) {
		try {
			return BookmarkTargetTypeRequest.valueOf(value);
		} catch (IllegalArgumentException e) {
			throw new BizException(BookmarkErrorCode.INVALID_BOOKMARK_TYPE);
		}
	}
}
