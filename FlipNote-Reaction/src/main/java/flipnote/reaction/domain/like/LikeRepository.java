package flipnote.reaction.domain.like;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LikeRepository {

	boolean existsByTargetTypeAndTargetIdAndUserId(LikeTargetType targetType, Long targetId, Long userId);

	Optional<Like> findByTargetTypeAndTargetIdAndUserId(LikeTargetType targetType, Long targetId, Long userId);

	Page<Like> findByTargetTypeAndUserId(LikeTargetType targetType, Long userId, Pageable pageable);

	Like save(Like like);

	void delete(Like like);
}
