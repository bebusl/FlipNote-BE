package flipnote.group.adapter.out.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import flipnote.group.adapter.out.entity.GroupEntity;
import flipnote.group.application.port.out.GroupRepositoryPort;
import flipnote.group.domain.model.group.Category;
import flipnote.group.domain.model.group.GroupInfo;
import flipnote.group.domain.policy.BusinessException;
import flipnote.group.domain.policy.ErrorCode;
import flipnote.group.infrastructure.persistence.jpa.GroupRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class GroupRepositoryAdapter implements GroupRepositoryPort {

	private final GroupRepository groupRepository;

	/**
	 * 그룹 저장
	 * @param group
	 * @return
	 */
	@Override
	public Long saveNewGroup(GroupEntity group) {
		return groupRepository.save(group).getId();
	}

	@Override
	public GroupEntity findById(Long id) {
		GroupEntity group = groupRepository.findById(id).orElseThrow(
			() -> new BusinessException(ErrorCode.GROUP_NOT_FOUND)
		);
		return group;
	}

	@Override
	public void delete(Long groupId) {
		if (!groupRepository.existsById(groupId)) {
			throw new BusinessException(ErrorCode.GROUP_NOT_FOUND);
		}
		groupRepository.deleteById(groupId);
	}

	@Override
	public List<GroupInfo> findAllByCursor(Long cursorId, Category category, int size, String groupName, Long userId) {
		return groupRepository.findAllByCursor(cursorId, category, size, groupName, userId);
	}

	@Override
	public List<GroupInfo> findAllByCursorAndUserId(Long cursorId, Category category, int size, Long userId, String groupName) {
		return groupRepository.findAllByCursorAndUserId(cursorId, category, size, userId, groupName);
	}

	@Override
	public List<GroupInfo> findAllByCursorAndCreatedUserId(Long cursorId, Category category, int size, Long userId, String groupName) {
		return groupRepository.findAllByCursorAndCreatedUserId(cursorId, category, size, userId, groupName);
	}

	@Override
	public boolean checkJoinable(Long groupId) {

		GroupEntity groupEntity = groupRepository.findByIdForUpdate(groupId).orElseThrow(
			() -> new BusinessException(ErrorCode.GROUP_NOT_FOUND)
		);

		 int maxMember = groupEntity.getMaxMember();
		 int count = groupEntity.getMemberCount();

		return maxMember > count;
	}

	@Override
	public List<GroupEntity> findAllById(List<Long> groupIds) {
		return groupRepository.findAllById(groupIds);
	}
}
