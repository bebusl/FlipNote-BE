package flipnote.group.application.service;

import org.springframework.stereotype.Service;

import flipnote.group.adapter.out.entity.GroupEntity;
import flipnote.group.application.port.in.CheckUserInGroupUseCase;
import flipnote.group.application.port.out.GroupMemberRepositoryPort;
import flipnote.group.application.port.out.GroupRepositoryPort;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CheckUserInGroupService implements CheckUserInGroupUseCase {

	private final GroupMemberRepositoryPort groupMemberRepository;
	private final GroupRepositoryPort groupRepository;

	@Override
	public boolean checkUserInGroup(Long userId, Long groupId) {

		GroupEntity group = groupRepository.findById(groupId);

		boolean isMember = groupMemberRepository.existsUserInGroup(groupId, userId);

		return isMember;
	}
}
