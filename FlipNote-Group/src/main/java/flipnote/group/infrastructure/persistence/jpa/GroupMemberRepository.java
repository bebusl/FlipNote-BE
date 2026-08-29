package flipnote.group.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import flipnote.group.adapter.out.entity.GroupMemberEntity;
import flipnote.group.domain.model.permission.GroupPermission;

public interface GroupMemberRepository
	extends JpaRepository<GroupMemberEntity, Long> {

	boolean existsByGroupIdAndUserId(Long groupId, Long userId);

	@Query("""
    select gm
    from GroupMemberEntity gm
    join fetch gm.role gr
    where gm.groupId = :groupId
""")
	List<GroupMemberEntity> findAllByGroupId(Long groupId);

	Optional<GroupMemberEntity> findByGroupIdAndUserId(Long groupId, Long userId);

	boolean existsByUserIdAndRole_Id(Long userId, Long id);

	@Query("""
    select gm.userId from GroupMemberEntity gm
    join PermissionEntity pe on pe.groupRoleId = gm.role.id
    where gm.groupId = :groupId and pe.permission = :permission
""")
	List<Long> findUserIdsByGroupIdAndPermission(Long groupId, GroupPermission permission);

	@Query("select gm.groupId from GroupMemberEntity gm where gm.userId = :userId")
	List<Long> findGroupIdsByUserId(@Param("userId") Long userId);
}
