package flipnote.group.application.port.out;

import java.util.List;

import flipnote.group.adapter.out.entity.GroupMemberEntity;
import flipnote.group.domain.model.member.MemberInfo;
import flipnote.group.domain.model.permission.GroupPermission;

public interface GroupMemberRepositoryPort {
    void save(GroupMemberEntity groupMember);

    boolean existsUserInGroup(Long groupId, Long userId);

    List<MemberInfo> findMemberInfo(Long groupId);

    GroupMemberEntity findMyRole(Long groupId, Long userId);

    boolean checkOwner(Long groupId, Long userId);

    void deleteGroupMember(Long memberId);

    boolean checkMember(Long memberId);

    GroupMemberEntity findById(Long memberId);

    List<Long> findUserIdsByPermission(Long groupId, GroupPermission permission);

    List<Long> findGroupIdsByUserId(Long userId);
}
