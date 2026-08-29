package flipnote.group.api.dto.request;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CursorPagingRequest {

	private String cursor;

	@Min(1)
	@Max(30)
	private Integer size = 10;

	private String sortBy;

	private String order = "desc";

	public Long getCursorId() {
		if (!StringUtils.hasText(cursor)) {
			return null;
		}

		final String normalized = cursor.trim();
		if (normalized.isEmpty()) {
			return null;
		}

		try {
			return Long.valueOf(normalized);
		} catch (NumberFormatException ex) {
			return null;
		}
	}

	public PageRequest getPageRequest() {
		if (sortBy == null || sortBy.isEmpty()) {
			return PageRequest.of(0, size);
		} else {
			Sort.Direction direction;
			try {
				direction = Sort.Direction.fromString(order);
			} catch (IllegalArgumentException e) {
				direction = Sort.Direction.DESC;
			}

			return PageRequest.of(0, size, Sort.by(direction, sortBy));
		}
	}
}
