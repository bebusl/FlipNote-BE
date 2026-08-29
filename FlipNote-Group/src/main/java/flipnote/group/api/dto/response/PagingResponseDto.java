package flipnote.group.api.dto.response;

import java.util.List;

import org.springframework.data.domain.Page;

public record PagingResponseDto<T>(
	List<T> content,
	int page,
	int size,
	long totalElements,
	int totalPages,
	boolean first,
	boolean last,
	boolean hasNext,
	boolean hasPrevious
) {

	public static <T> PagingResponseDto<T> from(Page<T> page) {
		return new PagingResponseDto<>(
			page.getContent(),
			page.getNumber(),
			page.getSize(),
			page.getTotalElements(),
			page.getTotalPages(),
			page.isFirst(),
			page.isLast(),
			page.hasNext(),
			page.hasPrevious()
		);
	}
}
