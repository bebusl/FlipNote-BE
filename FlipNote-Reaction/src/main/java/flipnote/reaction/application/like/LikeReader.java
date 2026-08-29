package flipnote.reaction.application.like;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import flipnote.reaction.domain.common.BizException;
import flipnote.reaction.domain.like.Like;
import flipnote.reaction.domain.like.LikeErrorCode;
import flipnote.reaction.domain.like.LikeRepository;
import flipnote.reaction.domain.like.LikeTargetType;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LikeReader {

	private final LikeRepository likeRepository;

	public Like findByTargetAndUserId(LikeTargetType targetType, Long targetId, Long userId) {
		return likeRepository.findByTargetTypeAndTargetIdAndUserId(targetType, targetId, userId)
			.orElseThrow(() -> new BizException(LikeErrorCode.LIKE_NOT_FOUND));
	}

	public boolean isLiked(LikeTargetType targetType, Long targetId, Long userId) {
		return likeRepository.existsByTargetTypeAndTargetIdAndUserId(targetType, targetId, userId);
	}

	public Map<Long, Boolean> areLiked(LikeTargetType targetType, List<Long> targetIds, long userId) {
		return targetIds.stream()
			.collect(Collectors.toMap(
				targetId -> targetId,
				targetId -> likeRepository.existsByTargetTypeAndTargetIdAndUserId(targetType, targetId, userId)
			));
	}
}
