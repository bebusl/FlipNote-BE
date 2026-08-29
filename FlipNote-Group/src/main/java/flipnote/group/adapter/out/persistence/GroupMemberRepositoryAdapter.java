package flipnote.group.adapter.out.persistence;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import flipnote.group.adapter.out.entity.GroupEntity;
import flipnote.group.adapter.out.entity.GroupMemberEntity;
import flipnote.group.application.port.out.GroupMemberRepositoryPort;
import flipnote.group.domain.model.member.GroupMemberRole;
import flipnote.group.domain.model.member.MemberInfo;
import flipnote.group.domain.model.permission.GroupPermission;
import flipnote.group.domain.policy.BusinessException;
import flipnote.group.domain.policy.ErrorCode;
import flipnote.group.infrastructure.persistence.jpa.GroupMemberRepository;
import flipnote.group.infrastructure.persistence.jpa.GroupRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class GroupMemberRepositoryAdapter implements GroupMemberRepositoryPort {

	private final GroupMemberRepository groupMemberRepository;
	private final GroupRepository groupRepository;

	/**
	 * 그룹 멤버 저장
	 * @param groupMember
	 */
	@Override
	public void save(GroupMemberEntity groupMember) {
		groupMemberRepository.save(groupMember);

		GroupEntity groupEntity = groupRepository.findByIdForUpdate(groupMember.getGroupId()).orElseThrow(
			() -> new BusinessException(ErrorCode.GROUP_NOT_FOUND)
		);

		//그룹 엔티티 + 1
		groupEntity.plusCount();
	}

	/**
	 * 유저가 그룹 내에 있는지 체크
	 * @param groupId
	 * @param userId
	 */
	@Override
	public boolean existsUserInGroup(Long groupId, Long userId) {

		boolean isMember = groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);

		return isMember;
	}

	/**
	 * 그룹 멤버 아이디 조회
	 * @param groupId
	 * @return
	 */
	@Override
	public List<MemberInfo> findMemberInfo(Long groupId) {
		List<GroupMemberEntity> entities = groupMemberRepository.findAllByGroupId(groupId);


		return entities.stream()
			.map(GroupMemberEntity::toMemberInfo)
			.collect(Collectors.toList());
	}

	@Override
	public boolean checkOwner(Long groupId, Long userId) {

		GroupMemberEntity groupMember = groupMemberRepository.findByGroupIdAndUserId(groupId, userId).orElseThrow(
			() -> new BusinessException(ErrorCode.USER_NOT_IN_GROUP)
		);

		return groupMember.getRole().equals(GroupMemberRole.OWNER);
	}

	@Override
	public GroupMemberEntity findMyRole(Long groupId, Long userId) {

		GroupMemberEntity entity = groupMemberRepository.findByGroupIdAndUserId(groupId, userId).orElseThrow(
			() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)
		);

		return entity;
	}

	@Override
	public void deleteGroupMember(Long memberId) {

		GroupMemberEntity groupMember = groupMemberRepository.findById(memberId).orElseThrow(
			() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)
		);

		groupMemberRepository.deleteById(memberId);

		//그룹 인원수 동기화
		GroupEntity group = groupRepository.findByIdForUpdate(groupMember.getGroupId()).orElseThrow(
			() -> new BusinessException(ErrorCode.GROUP_NOT_FOUND)
		);

		group.minusCount();

	}

	@Override
	public boolean checkMember(Long memberId) {
		return groupMemberRepository.existsById(memberId);
	}

	@Override
	public GroupMemberEntity findById(Long memberId) {
		return groupMemberRepository.findById(memberId).orElseThrow(
			() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)
		);
	}

	@Override
	public List<Long> findUserIdsByPermission(Long groupId, GroupPermission permission) {
		return groupMemberRepository.findUserIdsByGroupIdAndPermission(groupId, permission);
	}

	@Override
	public List<Long> findGroupIdsByUserId(Long userId) {
		return groupMemberRepository.findGroupIdsByUserId(userId);
	}
}
