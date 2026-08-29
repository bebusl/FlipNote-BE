package flipnote.group.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.group.adapter.out.entity.GroupMemberEntity;
import flipnote.group.application.port.in.MyPermissionUseCase;
import flipnote.group.application.port.in.command.MyPermissionCommand;
import flipnote.group.application.port.in.result.MyPermissionResult;
import flipnote.group.application.port.out.GroupMemberRepositoryPort;
import flipnote.group.application.port.out.GroupRoleRepositoryPort;
import flipnote.group.domain.model.permission.GroupPermission;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MyPermissionService implements MyPermissionUseCase {

	private final GroupMemberRepositoryPort groupMemberRepository;
	private final GroupRoleRepositoryPort groupRoleRepository;

	@Override
	@Transactional(readOnly = true)
	public MyPermissionResult findMyPermission(MyPermissionCommand cmd) {

		GroupMemberEntity groupMember = groupMemberRepository.findMyRole(cmd.groupId(), cmd.userId());

		List<GroupPermission> permissions = groupRoleRepository.findMyRolePermission(groupMember.getGroupId(), groupMember.getRole().getRole());

		return MyPermissionResult.of(groupMember.getRole().getRole(), permissions);
	}
}
