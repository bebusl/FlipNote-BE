package flipnote.reaction.domain.bookmark;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookmarkRepository {

	boolean existsByTargetTypeAndTargetIdAndUserId(BookmarkTargetType targetType, Long targetId, Long userId);

	Optional<Bookmark> findByTargetTypeAndTargetIdAndUserId(BookmarkTargetType targetType, Long targetId, Long userId);

	Page<Bookmark> findByTargetTypeAndUserId(BookmarkTargetType targetType, Long userId, Pageable pageable);

	Bookmark save(Bookmark bookmark);

	void delete(Bookmark bookmark);
}
