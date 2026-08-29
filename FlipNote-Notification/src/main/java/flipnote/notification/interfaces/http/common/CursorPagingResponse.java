package flipnote.notification.interfaces.http.common;

import java.util.List;
import java.util.Objects;

import flipnote.notification.application.dto.result.PagedResult;

public record CursorPagingResponse<T>(
	List<T> content,
	boolean hasNext,
	String nextCursor,
	int size
) {

	public static <T> CursorPagingResponse<T> from(PagedResult<T> pagedResult) {
		String nextCursor = Objects.toString(pagedResult.nextCursor(), null);
		return new CursorPagingResponse<>(
			pagedResult.content(),
			pagedResult.hasNext(),
			nextCursor,
			pagedResult.content().size()
		);
	}
}
