package flipnote.group.application.port.out;

import java.util.List;

import flipnote.group.adapter.out.entity.RoleEntity;
import flipnote.group.domain.model.member.GroupMemberRole;
import flipnote.group.domain.model.permission.GroupPermission;

public interface GroupRoleRepositoryPort {
	RoleEntity create(Long groupId);

	boolean checkRole(Long userId, Long groupId, GroupMemberRole groupMemberRole);

	boolean checkPermission(Long userId, Long groupId, GroupPermission permission);

	List<GroupPermission> addPermission(Long groupId, GroupMemberRole role, GroupPermission permission);

	boolean existPermission(GroupMemberRole groupMemberRole, Long aLong, GroupPermission permission);

	List<GroupPermission> removePermission(Long groupId, GroupMemberRole role, GroupPermission permission);

	List<GroupPermission> findMyRolePermission(Long groupId, GroupMemberRole role);

	RoleEntity findByGroupIdAndRole(Long groupId, GroupMemberRole groupMemberRole);

	GroupMemberRole findRole(Long userId, Long groupId);
}
