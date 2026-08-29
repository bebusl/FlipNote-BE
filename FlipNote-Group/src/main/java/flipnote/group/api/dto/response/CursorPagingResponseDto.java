package flipnote.group.api.dto.response;

import java.util.List;
import java.util.Objects;

public record CursorPagingResponseDto<T>(
	List<T> content,
	boolean hasNext,
	String nextCursor,
	int size
) {

	public static <T> CursorPagingResponseDto<T> of(List<T> content, boolean hasNext, String nextCursor) {
		return new CursorPagingResponseDto<>(content, hasNext, hasNext ? nextCursor : null, content.size());
	}

	public static <T> CursorPagingResponseDto<T> of(List<T> content, boolean hasNext, Long nextCursorId) {
		String nextCursor = Objects.toString(nextCursorId, null);
		return of(content, hasNext, nextCursor);
	}
}
