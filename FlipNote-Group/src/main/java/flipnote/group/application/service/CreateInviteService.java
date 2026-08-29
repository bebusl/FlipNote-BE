package flipnote.group.application.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.group.adapter.out.entity.GroupEntity;
import flipnote.group.adapter.out.entity.InviteEntity;
import flipnote.group.application.dto.GroupInviteMessage;
import flipnote.group.application.port.in.CreateInviteUseCase;
import flipnote.group.application.port.in.command.CreateInviteCommand;
import flipnote.group.application.port.in.result.CreateInviteResult;
import flipnote.group.application.port.out.GroupMemberRepositoryPort;
import flipnote.group.application.port.out.GroupRepositoryPort;
import flipnote.group.application.port.out.GroupRoleRepositoryPort;
import flipnote.group.application.port.out.InviteRepositoryPort;
import flipnote.group.application.port.out.NotificationMessagePort;
import flipnote.group.domain.event.GuestInviteCreatedEvent;
import flipnote.group.domain.model.invite.InviteStatus;
import flipnote.group.domain.model.permission.GroupPermission;
import flipnote.group.domain.policy.BusinessException;
import flipnote.group.domain.policy.ErrorCode;
import flipnote.user.grpc.GetUserByEmailRequest;
import flipnote.user.grpc.GetUserByEmailResponse;
import flipnote.user.grpc.UserQueryServiceGrpc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CreateInviteService implements CreateInviteUseCase {

	private final InviteRepositoryPort inviteRepository;
	private final GroupRoleRepositoryPort groupRoleRepository;
	private final GroupMemberRepositoryPort groupMemberRepository;
	private final GroupRepositoryPort groupRepository;
	private final UserQueryServiceGrpc.UserQueryServiceBlockingStub userQueryServiceStub;
	private final ApplicationEventPublisher eventPublisher;
	private final NotificationMessagePort notificationMessagePort;

	@Override
	@Transactional
	public CreateInviteResult createInvite(CreateInviteCommand cmd) {
		// 1. INVITE 권한 체크
		boolean hasPermission = groupRoleRepository.checkPermission(cmd.inviterUserId(), cmd.groupId(), GroupPermission.INVITE);
		if (!hasPermission) {
			throw new BusinessException(ErrorCode.INVITE_PERMISSION_DENIED);
		}

		// 2. 자기 자신 초대 방지
		if (cmd.inviterEmail().equals(cmd.inviteeEmail())) {
			throw new BusinessException(ErrorCode.INVITE_SELF_NOT_ALLOWED);
		}

		// 3. gRPC로 이메일 → 유저 조회
		GetUserByEmailResponse userResponse;
		try {
			userResponse = userQueryServiceStub.getUserByEmail(
				GetUserByEmailRequest.newBuilder()
					.setEmail(cmd.inviteeEmail())
					.build()
			);
		} catch (Exception ex) {
			log.error("createInvite user grpc getUserByEmail", ex);
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
		}

		Long invitationId;

		if (userResponse.getExists()) {
			// 4. 유저 존재시: 회원 초대
			invitationId = createUserInvitation(
				cmd.inviterUserId(), cmd.groupId(),
				userResponse.getUser().getId(), cmd.inviteeEmail()
			);
		} else {
			// 5. 유저 미존재시: 비회원 초대 + 이메일 발송
			invitationId = createGuestInvitation(
				cmd.inviterUserId(), cmd.groupId(), cmd.inviteeEmail()
			);
		}

		return new CreateInviteResult(invitationId);
	}

	private Long createUserInvitation(Long inviterUserId, Long groupId, Long inviteeUserId, String inviteeEmail) {
		// 이미 멤버인지 체크
		if (groupMemberRepository.existsUserInGroup(groupId, inviteeUserId)) {
			throw new BusinessException(ErrorCode.MEMBER_ALREADY_JOINED);
		}

		// 이미 초대된 유저인지 체크
		if (inviteRepository.existsByGroupIdAndInviteeUserIdAndStatus(groupId, inviteeUserId, InviteStatus.PENDING)) {
			throw new BusinessException(ErrorCode.INVITE_ALREADY_EXISTS);
		}

		InviteEntity invite = InviteEntity.create(groupId, inviterUserId, inviteeUserId, inviteeEmail);
		inviteRepository.save(invite);

		try {
			GroupEntity group = groupRepository.findById(groupId);
			notificationMessagePort.sendGroupInvite(new GroupInviteMessage(groupId, inviteeUserId, group.getName()));
		} catch (Exception e) {
			log.error("GroupInviteMessage 발행 실패: groupId={}, inviteeId={}", groupId, inviteeUserId, e);
		}

		return invite.getId();
	}

	private Long createGuestInvitation(Long inviterUserId, Long groupId, String inviteeEmail) {
		// 이미 이메일로 초대된 경우 체크
		if (inviteRepository.existsByGroupIdAndInviteeEmailAndStatus(groupId, inviteeEmail, InviteStatus.PENDING)) {
			throw new BusinessException(ErrorCode.INVITE_ALREADY_EXISTS);
		}

		InviteEntity invite = InviteEntity.create(groupId, inviterUserId, null, inviteeEmail);
		inviteRepository.save(invite);

		// 그룹 이름 조회 후 이메일 발송 이벤트 발행
		GroupEntity group = groupRepository.findById(groupId);
		eventPublisher.publishEvent(new GuestInviteCreatedEvent(inviteeEmail, group.getName()));

		return invite.getId();
	}
}
