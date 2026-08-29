package flipnote.group.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.group.adapter.out.entity.InviteEntity;
import flipnote.group.application.port.in.CancelInviteUseCase;
import flipnote.group.application.port.in.command.CancelInviteCommand;
import flipnote.group.application.port.out.GroupRoleRepositoryPort;
import flipnote.group.application.port.out.InviteRepositoryPort;
import flipnote.group.domain.model.invite.InviteStatus;
import flipnote.group.domain.model.permission.GroupPermission;
import flipnote.group.domain.policy.BusinessException;
import flipnote.group.domain.policy.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CancelInviteService implements CancelInviteUseCase {

	private final InviteRepositoryPort inviteRepository;
	private final GroupRoleRepositoryPort groupRoleRepository;

	private static final GroupPermission INVITE_PERMISSION = GroupPermission.INVITE;

	@Override
	@Transactional
	public void cancelInvite(CancelInviteCommand cmd) {
		// INVITE 권한 체크
		boolean hasPermission = groupRoleRepository.checkPermission(cmd.userId(), cmd.groupId(), INVITE_PERMISSION);
		if (!hasPermission) {
			throw new BusinessException(ErrorCode.INVITE_PERMISSION_DENIED);
		}

		// PENDING 초대 찾기
		InviteEntity invite = inviteRepository.findByIdAndStatus(cmd.invitationId(), InviteStatus.PENDING);

		// 삭제
		inviteRepository.delete(invite);
	}
}
