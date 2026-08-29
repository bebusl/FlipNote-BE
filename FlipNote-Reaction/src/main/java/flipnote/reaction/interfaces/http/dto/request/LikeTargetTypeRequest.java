package flipnote.reaction.interfaces.http.dto.request;

import flipnote.reaction.domain.common.BizException;
import flipnote.reaction.domain.like.LikeErrorCode;
import flipnote.reaction.domain.like.LikeTargetType;

public enum LikeTargetTypeRequest {
	card_set;

	public LikeTargetType toEntity() {
		return switch (this) {
			case card_set -> LikeTargetType.CARD_SET;
		};
	}

	public static LikeTargetTypeRequest from(String value) {
		try {
			return LikeTargetTypeRequest.valueOf(value);
		} catch (IllegalArgumentException e) {
			throw new BizException(LikeErrorCode.INVALID_LIKE_TYPE);
		}
	}
}
