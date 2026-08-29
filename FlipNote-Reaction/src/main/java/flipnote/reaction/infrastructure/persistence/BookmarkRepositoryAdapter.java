package flipnote.reaction.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import flipnote.reaction.domain.bookmark.Bookmark;
import flipnote.reaction.domain.bookmark.BookmarkRepository;
import flipnote.reaction.domain.bookmark.BookmarkTargetType;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class BookmarkRepositoryAdapter implements BookmarkRepository {

	private final SpringDataBookmarkRepository springDataBookmarkRepository;

	@Override
	public boolean existsByTargetTypeAndTargetIdAndUserId(BookmarkTargetType targetType, Long targetId, Long userId) {
		return springDataBookmarkRepository.existsByTargetTypeAndTargetIdAndUserId(targetType, targetId, userId);
	}

	@Override
	public Optional<Bookmark> findByTargetTypeAndTargetIdAndUserId(BookmarkTargetType targetType, Long targetId,
		Long userId) {
		return springDataBookmarkRepository.findByTargetTypeAndTargetIdAndUserId(targetType, targetId, userId);
	}

	@Override
	public Page<Bookmark> findByTargetTypeAndUserId(BookmarkTargetType targetType, Long userId, Pageable pageable) {
		return springDataBookmarkRepository.findByTargetTypeAndUserId(targetType, userId, pageable);
	}

	@Override
	public Bookmark save(Bookmark bookmark) {
		return springDataBookmarkRepository.save(bookmark);
	}

	@Override
	public void delete(Bookmark bookmark) {
		springDataBookmarkRepository.delete(bookmark);
	}
}
