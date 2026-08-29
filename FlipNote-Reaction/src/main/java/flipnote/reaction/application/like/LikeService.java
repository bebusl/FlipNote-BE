package flipnote.reaction.application.like;

import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.reaction.application.common.CardSetSummaryResult;
import flipnote.reaction.domain.common.BizException;
import flipnote.reaction.domain.common.CommonErrorCode;
import flipnote.reaction.domain.like.Like;
import flipnote.reaction.domain.like.LikeErrorCode;
import flipnote.reaction.domain.like.LikeRepository;
import flipnote.reaction.domain.like.LikeTargetType;
import flipnote.reaction.domain.like.event.LikeAddedEvent;
import flipnote.reaction.domain.like.event.LikeRemovedEvent;
import flipnote.reaction.infrastructure.grpc.CardSetGrpcClient;
import flipnote.reaction.interfaces.http.dto.request.LikeSearchRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LikeService {

	private final LikeRepository likeRepository;
	private final LikeReader likeReader;
	private final ApplicationEventPublisher eventPublisher;
	private final CardSetGrpcClient cardSetGrpcClient;

	@Transactional(noRollbackFor = DataIntegrityViolationException.class)
	public Long addLike(LikeTargetType targetType, Long targetId, Long userId) {
		if (!cardSetGrpcClient.isCardSetViewable(targetId, userId)) {
			throw new BizException(CommonErrorCode.TARGET_NOT_VIEWABLE);
		}

		if (likeReader.isLiked(targetType, targetId, userId)) {
			throw new BizException(LikeErrorCode.ALREADY_LIKED);
		}

		Like like = Like.builder()
			.targetType(targetType)
			.targetId(targetId)
			.userId(userId)
			.build();

		try {
			likeRepository.save(like);
		} catch (DataIntegrityViolationException e) {
			throw new BizException(LikeErrorCode.ALREADY_LIKED);
		}

		eventPublisher.publishEvent(new LikeAddedEvent(targetType.name(), targetId, userId));

		return like.getId();
	}

	@Transactional
	public void removeLike(LikeTargetType targetType, Long targetId, Long userId) {
		Like like = likeReader.findByTargetAndUserId(targetType, targetId, userId);
		likeRepository.delete(like);

		eventPublisher.publishEvent(new LikeRemovedEvent(targetType.name(), targetId, userId));
	}

	public Page<LikeResult> getLikes(LikeTargetType targetType, Long userId, LikeSearchRequest request) {
		Page<Like> likePage = likeRepository.findByTargetTypeAndUserId(
			targetType, userId, request.getPageRequest()
		);

		List<Long> targetIds = likePage.getContent().stream()
			.map(Like::getTargetId)
			.toList();

		Map<Long, CardSetSummaryResult> cardSetMap = fetchCardSets(targetIds, userId);

		return likePage.map(l -> LikeResult.from(l, cardSetMap.get(l.getTargetId())));
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
