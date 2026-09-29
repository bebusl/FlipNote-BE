package flipnote.group.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import flipnote.group.adapter.out.entity.GroupMemberEntity;
import flipnote.group.application.port.in.KickMemberUseCase;
import flipnote.group.application.port.in.command.KickMemberCommand;
import flipnote.group.application.port.out.GroupMemberRepositoryPort;
import flipnote.group.application.port.out.GroupRoleRepositoryPort;
import flipnote.group.domain.model.permission.GroupPermission;
import flipnote.group.domain.policy.BusinessException;
import flipnote.group.domain.policy.ErrorCode;
import flipnote.group.domain.event.GroupMemberKickedEvent;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KickMemberService implements KickMemberUseCase {

	private final GroupMemberRepositoryPort groupMemberRepository;
	private final GroupRoleRepositoryPort groupRoleRepository;
	private final ApplicationEventPublisher eventPublisher;

	@Override
	@Transactional
	public void kickMember(KickMemberCommand cmd) {
		//권한 체크
		boolean hasPermission = groupRoleRepository.checkPermission(cmd.userId(), cmd.groupId(), GroupPermission.MEMBER_MANAGE);
		if(!hasPermission) {
			throw new BusinessException(ErrorCode.PERMISSION_DENIED);
		}

		GroupMemberEntity member = groupMemberRepository.findById(cmd.memberId());
		if(!member.getGroupId().equals(cmd.groupId())) {
			throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND);
		}

		groupMemberRepository.deleteGroupMember(cmd.memberId());
		eventPublisher.publishEvent(new GroupMemberKickedEvent(cmd.groupId(), member.getUserId()));

	}
}
