package flipnote.group.application.port.in;

import org.springframework.data.domain.Pageable;

import flipnote.group.api.dto.response.PagingResponseDto;
import flipnote.group.application.port.in.command.FindOutgoingInviteCommand;
import flipnote.group.domain.model.invite.InviteInfo;
import flipnote.group.domain.model.invite.InviteMyInfo;

public interface FindInviteUseCase {
	PagingResponseDto<InviteInfo> findOutgoingInvites(FindOutgoingInviteCommand cmd);

	PagingResponseDto<InviteMyInfo> findIncomingInvites(Long inviteeUserId, Pageable pageable);
}
