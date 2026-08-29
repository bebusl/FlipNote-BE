package flipnote.group.api.dto.request;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PagingRequest {

	@Min(1)
	private Integer page = 1;

	@Min(1)
	@Max(30)
	private Integer size = 10;

	private String sortBy;

	private String order = "desc";

	public PageRequest getPageRequest() {
		if (sortBy == null || sortBy.isEmpty()) {
			return PageRequest.of(page - 1, size);
		} else {
			return PageRequest.of(page - 1, size, Sort.by(getDirection(), sortBy));
		}
	}

	public Sort.Direction getDirection() {
		Sort.Direction direction;
		try {
			direction = Sort.Direction.fromString(order);
		} catch (IllegalArgumentException e) {
			direction = Sort.Direction.DESC;
		}
		return direction;
	}
}
