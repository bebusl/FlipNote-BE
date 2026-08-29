package flipnote.reaction.interfaces.http;

import static flipnote.reaction.interfaces.http.common.HeaderConstants.USER_ID;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import flipnote.reaction.application.bookmark.BookmarkResult;
import flipnote.reaction.application.bookmark.BookmarkService;
import flipnote.reaction.domain.bookmark.BookmarkTargetType;
import flipnote.reaction.interfaces.http.common.IdResponse;
import flipnote.reaction.interfaces.http.common.PagingResponse;
import flipnote.reaction.interfaces.http.dto.request.BookmarkSearchRequest;
import flipnote.reaction.interfaces.http.dto.request.BookmarkTargetTypeRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/v1/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

	private final BookmarkService bookmarkService;

	@PostMapping("/{targetType}/{targetId}")
	public IdResponse addBookmark(
		@RequestHeader(USER_ID) Long userId,
		@PathVariable String targetType,
		@PathVariable Long targetId
	) {
		BookmarkTargetType type = BookmarkTargetTypeRequest.from(targetType).toEntity();
		return IdResponse.from(bookmarkService.addBookmark(type, targetId, userId));
	}

	@DeleteMapping("/{targetType}/{targetId}")
	public void removeBookmark(
		@RequestHeader(USER_ID) Long userId,
		@PathVariable String targetType,
		@PathVariable Long targetId
	) {
		BookmarkTargetType type = BookmarkTargetTypeRequest.from(targetType).toEntity();
		bookmarkService.removeBookmark(type, targetId, userId);
	}

	@GetMapping("/{targetType}")
	public PagingResponse<BookmarkResult> getBookmarks(
		@RequestHeader(USER_ID) Long userId,
		@PathVariable String targetType,
		@Valid @ModelAttribute BookmarkSearchRequest request
	) {
		BookmarkTargetType type = BookmarkTargetTypeRequest.from(targetType).toEntity();
		return PagingResponse.from(bookmarkService.getBookmarks(type, userId, request));
	}
}
