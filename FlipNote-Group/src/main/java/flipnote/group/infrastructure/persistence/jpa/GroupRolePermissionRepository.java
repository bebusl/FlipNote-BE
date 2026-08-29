package flipnote.group.infrastructure.persistence.jpa;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import flipnote.group.adapter.out.entity.PermissionEntity;
import flipnote.group.domain.model.permission.GroupPermission;
import flipnote.group.infrastructure.persistence.querydsl.GroupRolePermissionCustom;

public interface GroupRolePermissionRepository extends JpaRepository<PermissionEntity, Long>,
	GroupRolePermissionCustom {
	boolean existsByGroupRoleIdAndPermission(Long id, GroupPermission permission);

	List<PermissionEntity> findAllByGroupRoleId(Long id);

	void deleteByGroupRoleIdAndPermission(Long id, GroupPermission permission);
}
