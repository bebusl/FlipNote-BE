package flipnote.group.api.dto.request;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import flipnote.group.domain.model.group.Category;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class GroupListRequestDto extends CursorPagingRequest {

	private Category category;
	private String groupName;

	@Override
	public PageRequest getPageRequest() {
		return PageRequest.of(0, getSize(), Sort.by(Sort.Direction.DESC, "id"));
	}
}
