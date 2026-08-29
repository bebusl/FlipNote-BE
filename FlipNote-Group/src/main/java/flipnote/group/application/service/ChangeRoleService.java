package flipnote.group.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.group.adapter.out.entity.GroupMemberEntity;
import flipnote.group.adapter.out.entity.RoleEntity;
import flipnote.group.application.port.in.ChangeRoleUseCase;
import flipnote.group.application.port.in.command.ChangeRoleCommand;
import flipnote.group.application.port.in.result.ChangeRoleResult;
import flipnote.group.application.port.out.GroupMemberRepositoryPort;
import flipnote.group.application.port.out.GroupRoleRepositoryPort;
import flipnote.group.domain.model.member.GroupMemberRole;
import flipnote.group.domain.policy.BusinessException;
import flipnote.group.domain.policy.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChangeRoleService implements ChangeRoleUseCase {

	private final GroupMemberRepositoryPort groupMemberRepository;
	private final GroupRoleRepositoryPort groupRoleRepository;

	@Override
	@Transactional
	public ChangeRoleResult ChangeRole(ChangeRoleCommand cmd) {
		//호스트의 역할체크
		GroupMemberRole role = groupRoleRepository.findRole(cmd.userId(), cmd.groupId());

		if(!role.isHigherThan(cmd.changeRole())) {
			throw new BusinessException(ErrorCode.PERMISSION_ROLE_TOO_LOW);
		}

		GroupMemberEntity groupMember = groupMemberRepository.findById(cmd.memberId());

		//호스트의 역할이 바꾸는 멤버의 역할보다 낮은 경우
		if(!role.isHigherThan(groupMember.getRole().getRole())) {
			throw new BusinessException(ErrorCode.PERMISSION_ROLE_TOO_LOW);
		}

		RoleEntity roleEntity = groupRoleRepository.findByGroupIdAndRole(cmd.groupId(), cmd.changeRole());

		groupMember.changeRole(roleEntity);

		return ChangeRoleResult.of(groupMember);
	}
}
