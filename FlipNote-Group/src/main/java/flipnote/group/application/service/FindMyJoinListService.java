package flipnote.group.application.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import flipnote.group.adapter.out.entity.GroupEntity;
import flipnote.group.adapter.out.entity.JoinEntity;
import flipnote.group.application.port.in.FindMyJoinListUseCase;
import flipnote.group.application.port.in.result.FindMyJoinListResult;
import flipnote.group.application.port.out.GroupRepositoryPort;
import flipnote.group.application.port.out.JoinRepositoryPort;
import flipnote.group.domain.model.join.JoinMyInfo;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FindMyJoinListService implements FindMyJoinListUseCase {

	private final JoinRepositoryPort joinRepository;
	private final GroupRepositoryPort groupRepository;

	@Override
	public FindMyJoinListResult findMyJoinList(Long userId) {

		List<JoinEntity> joinList = joinRepository.findMyJoinList(userId);

		List<Long> groupIds = joinList.stream()
			.map(JoinEntity::getGroupId)
			.toList();

		List<GroupEntity> groupList = groupRepository.findAllById(groupIds);

		Map<Long, String> groupNameMap = groupList.stream()
			.collect(Collectors.toMap(GroupEntity::getId, GroupEntity::getName));

		List<JoinMyInfo> result = joinList.stream()
			.map(join -> JoinMyInfo.of(join, groupNameMap.get(join.getGroupId())))
			.toList();


		return FindMyJoinListResult.of(result);
	}
}
