package flipnote.group.infrastructure.persistence.querydsl;

import org.springframework.stereotype.Repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;

import flipnote.group.adapter.out.entity.QGroupMemberEntity;
import flipnote.group.adapter.out.entity.QPermissionEntity;
import flipnote.group.adapter.out.entity.QRoleEntity;
import flipnote.group.domain.model.permission.GroupPermission;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class GroupRolePermissionRepositoryImpl implements GroupRolePermissionCustom {

	private final JPAQueryFactory queryFactory;

	QRoleEntity role = QRoleEntity.roleEntity;
	QPermissionEntity permission = QPermissionEntity.permissionEntity;
	QGroupMemberEntity groupMember = QGroupMemberEntity.groupMemberEntity;

	/**
	 * 유저가 특정 그룹의 권한 존재하는지 체크
	 * @param groupId
	 * @param userId
	 * @param groupPermission
	 * @return
	 */
	@Override
	public boolean existsUserInGroupPermission(Long groupId, Long userId, GroupPermission groupPermission) {

		return queryFactory
			.selectOne()
			.from(groupMember)
			.join(role).on(role.id.eq(groupMember.role.id))
			.join(permission).on(permission.groupRoleId.eq(role.id))
			.where(
				groupMember.userId.eq(userId),
				groupMember.groupId.eq(groupId),
				role.groupId.eq(groupId),
				permission.permission.eq(groupPermission)
			)
			.fetchFirst() != null;
	}
}
