package flipnote.group.application.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.group.application.port.in.AddPermissionUseCase;
import flipnote.group.application.port.in.command.PermissionCommand;
import flipnote.group.application.port.in.result.AddPermissionResult;
import flipnote.group.application.port.out.GroupRoleRepositoryPort;
import flipnote.group.domain.model.member.GroupMemberRole;
import flipnote.group.domain.model.permission.GroupPermission;
import flipnote.group.domain.policy.BusinessException;
import flipnote.group.domain.policy.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AddPermissionService implements AddPermissionUseCase {

	private final GroupRoleRepositoryPort groupRoleRepository;

	/**
	 * 권한 추가
	 * @param cmd
	 * @return
	 */
	@Override
	@Transactional
	public AddPermissionResult addPermission(PermissionCommand cmd) {

		GroupMemberRole role = groupRoleRepository.findRole(cmd.userId(), cmd.groupId());

		//호스트의 권한이 있는지
		boolean existHostPermission = groupRoleRepository.checkPermission(cmd.userId(), cmd.groupId(), cmd.permission());

		if(!existHostPermission) {
			throw new BusinessException(ErrorCode.PERMISSION_DENIED);
		}

		//권한이 낮을 경우
		if(!role.isHigherThan(cmd.changeRole())) {
			throw new BusinessException(ErrorCode.PERMISSION_DENIED);
		}

		boolean existPermission = groupRoleRepository.existPermission(cmd.changeRole(), cmd.groupId(), cmd.permission());

		if(existPermission) {
			throw new BusinessException(ErrorCode.PERMISSION_ALREADY_EXISTS);
		}

		List<GroupPermission> groupPermissions = groupRoleRepository.addPermission(cmd.groupId(), cmd.changeRole(),
			cmd.permission());

		return AddPermissionResult.of(groupPermissions, cmd.changeRole());
	}
}
