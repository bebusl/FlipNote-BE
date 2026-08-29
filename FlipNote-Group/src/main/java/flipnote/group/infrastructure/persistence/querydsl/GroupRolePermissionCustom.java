package flipnote.group.infrastructure.persistence.querydsl;

import flipnote.group.domain.model.permission.GroupPermission;

public interface GroupRolePermissionCustom {
	boolean existsUserInGroupPermission(Long groupId, Long userId, GroupPermission permission);
}
