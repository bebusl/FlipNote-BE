package flipnote.notification.application.dto.result;

import java.util.List;

public record PagedResult<T>(
	List<T> content,
	boolean hasNext,
	Long nextCursor
) {

	public static <T> PagedResult<T> of(List<T> content, boolean hasNext, Long nextCursor) {
		return new PagedResult<>(content, hasNext, hasNext ? nextCursor : null);
	}
}
