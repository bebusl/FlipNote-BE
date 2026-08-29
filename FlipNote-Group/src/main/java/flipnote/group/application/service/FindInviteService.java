package flipnote.group.application.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.group.adapter.out.entity.InviteEntity;
import flipnote.group.api.dto.response.PagingResponseDto;
import flipnote.group.application.port.in.FindInviteUseCase;
import flipnote.group.application.port.in.command.FindOutgoingInviteCommand;
import flipnote.group.application.port.out.GroupRoleRepositoryPort;
import flipnote.group.application.port.out.InviteRepositoryPort;
import flipnote.group.domain.model.invite.InviteInfo;
import flipnote.group.domain.model.invite.InviteMyInfo;
import flipnote.group.domain.model.permission.GroupPermission;
import flipnote.group.domain.policy.BusinessException;
import flipnote.group.domain.policy.ErrorCode;
import flipnote.user.grpc.GetUserResponse;
import flipnote.user.grpc.GetUsersRequest;
import flipnote.user.grpc.GetUsersResponse;
import flipnote.user.grpc.UserQueryServiceGrpc;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FindInviteService implements FindInviteUseCase {

	private final InviteRepositoryPort inviteRepository;
	private final GroupRoleRepositoryPort groupRoleRepository;
	private final UserQueryServiceGrpc.UserQueryServiceBlockingStub userQueryServiceStub;

	private static final GroupPermission INVITE_PERMISSION = GroupPermission.INVITE;

	@Override
	public PagingResponseDto<InviteInfo> findOutgoingInvites(FindOutgoingInviteCommand cmd) {
		// INVITE 권한 체크
		boolean hasPermission = groupRoleRepository.checkPermission(cmd.userId(), cmd.groupId(), INVITE_PERMISSION);
		if (!hasPermission) {
			throw new BusinessException(ErrorCode.INVITE_PERMISSION_DENIED);
		}

		Page<InviteEntity> invitePage = inviteRepository.findAllByGroupId(cmd.groupId(), cmd.pageable());

		// inviteeUserId 목록 추출 (null 제외 - 비회원 초대)
		List<Long> inviteeUserIds = invitePage.getContent().stream()
			.map(InviteEntity::getInviteeUserId)
			.filter(Objects::nonNull)
			.toList();

		// gRPC로 유저 닉네임 한번에 조회
		Map<Long, String> idAndNicknames = Map.of();
		if (!inviteeUserIds.isEmpty()) {
			GetUsersResponse usersResponse = userQueryServiceStub.getUsers(
				GetUsersRequest.newBuilder()
					.addAllUserIds(inviteeUserIds)
					.build()
			);
			idAndNicknames = usersResponse.getUsersList().stream()
				.collect(Collectors.toMap(GetUserResponse::getId, GetUserResponse::getNickname));
		}

		Map<Long, String> finalIdAndNicknames = idAndNicknames;
		Page<InviteInfo> res = invitePage.map(invite -> {
			String nickname;
			if (invite.getInviteeUserId() == null) {
				nickname = invite.getInviteeEmail();
			} else {
				nickname = finalIdAndNicknames.getOrDefault(invite.getInviteeUserId(), "");
			}
			return InviteInfo.of(invite, nickname);
		});

		return PagingResponseDto.from(res);
	}

	@Override
	public PagingResponseDto<InviteMyInfo> findIncomingInvites(Long inviteeUserId, Pageable pageable) {
		Page<InviteEntity> invitePage = inviteRepository.findAllByInviteeUserId(inviteeUserId, pageable);

		Page<InviteMyInfo> res = invitePage.map(InviteMyInfo::of);
		return PagingResponseDto.from(res);
	}
}
