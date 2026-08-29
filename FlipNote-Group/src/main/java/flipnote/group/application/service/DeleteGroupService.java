package flipnote.group.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.group.application.port.in.DeleteGroupUseCase;
import flipnote.group.application.port.in.command.DeleteGroupCommand;
import flipnote.group.application.port.out.GroupRepositoryPort;
import flipnote.group.application.port.out.GroupRoleRepositoryPort;
import flipnote.group.domain.model.member.GroupMemberRole;
import flipnote.group.domain.policy.BusinessException;
import flipnote.group.domain.policy.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteGroupService implements DeleteGroupUseCase {

	private final GroupRepositoryPort groupRepository;
	private final GroupRoleRepositoryPort groupRoleRepository;

	@Override
	@Transactional
	public void deleteGroup(DeleteGroupCommand cmd) {

		//오너 인지 확인
		boolean isOwner = groupRoleRepository.checkRole(cmd.userId(), cmd.groupId(), GroupMemberRole.OWNER);

		//오너가 아닐 경우 에러 발생
		if(!isOwner) {
			throw new BusinessException(ErrorCode.NOT_OWNER);
		}

		groupRepository.delete(cmd.groupId());
	}

}
