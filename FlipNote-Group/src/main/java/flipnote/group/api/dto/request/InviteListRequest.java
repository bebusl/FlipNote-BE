package flipnote.group.api.dto.request;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public class InviteListRequest extends PagingRequest {

	@Override
	public PageRequest getPageRequest() {
		return PageRequest.of(getPage() - 1, getSize(), Sort.by(Sort.Direction.DESC, "id"));
	}
}
