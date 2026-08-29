package flipnote.group.application.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.group.application.port.in.FindGroupMemberUseCase;
import flipnote.group.application.port.in.command.FindGroupMemberCommand;
import flipnote.group.application.port.in.result.FindGroupMemberResult;
import flipnote.group.application.port.out.GroupMemberRepositoryPort;
import flipnote.group.domain.model.member.MemberInfo;
import flipnote.group.domain.policy.BusinessException;
import flipnote.group.domain.policy.ErrorCode;
import flipnote.user.grpc.GetUserResponse;
import flipnote.user.grpc.GetUsersRequest;
import flipnote.user.grpc.GetUsersResponse;
import flipnote.user.grpc.UserQueryServiceGrpc;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FindGroupMemberService implements FindGroupMemberUseCase {

	private final GroupMemberRepositoryPort groupMemberRepository;
	private final UserQueryServiceGrpc.UserQueryServiceBlockingStub userQueryService;

	/**
	 * 그룹 멤버 조회
	 * @param cmd
	 * @return
	 */
	@Override
	@Transactional(readOnly = true)
	public FindGroupMemberResult findGroupMember(FindGroupMemberCommand cmd) {

		List<MemberInfo> memberInfoList = groupMemberRepository.findMemberInfo(cmd.groupId());

		List<Long> ids = memberInfoList.stream()
			.map(MemberInfo::getUserId)
			.collect(Collectors.toList());

		GetUsersRequest req = GetUsersRequest.newBuilder()
			.addAllUserIds(ids)
			.build();

		GetUsersResponse res;
		try {
			res = userQueryService.getUsers(req);
		} catch (StatusRuntimeException e) {
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}

		// 맵으로 변환
		Map<Long, GetUserResponse> userInfoMap = res.getUsersList().stream()
			.collect(Collectors.toMap(GetUserResponse::getId, u -> u));

		// memberInfo에 nickname, profileImage 업데이트
		memberInfoList.forEach(memberInfo -> {
			GetUserResponse userInfo = userInfoMap.get(memberInfo.getUserId());
			if (userInfo != null) {
				memberInfo.updateUserInfo(userInfo.getNickname(), userInfo.getProfileImageUrl());
			}
		});

		return new FindGroupMemberResult(memberInfoList);
	}
}
