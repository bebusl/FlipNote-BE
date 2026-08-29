package flipnote.reaction.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import flipnote.reaction.domain.like.Like;
import flipnote.reaction.domain.like.LikeTargetType;

interface SpringDataLikeRepository extends JpaRepository<Like, Long> {

	boolean existsByTargetTypeAndTargetIdAndUserId(LikeTargetType targetType, Long targetId, Long userId);

	Optional<Like> findByTargetTypeAndTargetIdAndUserId(LikeTargetType targetType, Long targetId, Long userId);

	Page<Like> findByTargetTypeAndUserId(LikeTargetType targetType, Long userId, Pageable pageable);
}
