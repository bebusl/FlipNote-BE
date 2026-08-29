package flipnote.reaction.domain.like;

import flipnote.reaction.domain.common.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LikeErrorCode implements ErrorCode {
	INVALID_LIKE_TYPE(400, "LIKE_001", "유효하지 않은 좋아요 대상 타입입니다."),
	ALREADY_LIKED(409, "LIKE_002", "이미 좋아요한 대상입니다."),
	LIKE_NOT_FOUND(404, "LIKE_003", "좋아요를 찾을 수 없습니다.");

	private final int status;
	private final String code;
	private final String message;
}
