package flipnote.reaction.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import flipnote.reaction.domain.like.Like;
import flipnote.reaction.domain.like.LikeRepository;
import flipnote.reaction.domain.like.LikeTargetType;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class LikeRepositoryAdapter implements LikeRepository {

	private final SpringDataLikeRepository springDataLikeRepository;

	@Override
	public boolean existsByTargetTypeAndTargetIdAndUserId(LikeTargetType targetType, Long targetId, Long userId) {
		return springDataLikeRepository.existsByTargetTypeAndTargetIdAndUserId(targetType, targetId, userId);
	}

	@Override
	public Optional<Like> findByTargetTypeAndTargetIdAndUserId(LikeTargetType targetType, Long targetId, Long userId) {
		return springDataLikeRepository.findByTargetTypeAndTargetIdAndUserId(targetType, targetId, userId);
	}

	@Override
	public Page<Like> findByTargetTypeAndUserId(LikeTargetType targetType, Long userId, Pageable pageable) {
		return springDataLikeRepository.findByTargetTypeAndUserId(targetType, userId, pageable);
	}

	@Override
	public Like save(Like like) {
		return springDataLikeRepository.save(like);
	}

	@Override
	public void delete(Like like) {
		springDataLikeRepository.delete(like);
	}
}
