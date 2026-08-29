package flipnote.reaction.domain.bookmark;

import flipnote.reaction.domain.common.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BookmarkErrorCode implements ErrorCode {
	INVALID_BOOKMARK_TYPE(400, "BOOKMARK_001", "유효하지 않은 북마크 대상 타입입니다."),
	ALREADY_BOOKMARKED(409, "BOOKMARK_002", "이미 북마크한 대상입니다."),
	BOOKMARK_NOT_FOUND(404, "BOOKMARK_003", "북마크를 찾을 수 없습니다.");

	private final int status;
	private final String code;
	private final String message;
}
