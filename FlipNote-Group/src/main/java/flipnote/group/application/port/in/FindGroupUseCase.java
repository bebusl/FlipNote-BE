package flipnote.group.application.port.in;

import java.util.List;

import flipnote.group.api.dto.request.GroupListRequestDto;
import flipnote.group.api.dto.response.CursorPagingResponseDto;
import flipnote.group.application.port.in.command.FindGroupCommand;
import flipnote.group.application.port.in.result.FindGroupResult;
import flipnote.group.domain.model.group.GroupInfo;

public interface FindGroupUseCase {
	FindGroupResult findGroup(FindGroupCommand cmd);

	CursorPagingResponseDto<GroupInfo> findAllGroup(Long userId, GroupListRequestDto req);

	CursorPagingResponseDto<GroupInfo> findMyGroup(Long userId, GroupListRequestDto req);

	CursorPagingResponseDto<GroupInfo> findCreatedGroup(Long userId, GroupListRequestDto req);

	List<Long> findMyGroup(Long userId);
}
