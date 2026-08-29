package flipnote.group.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.group.adapter.out.entity.GroupMemberEntity;
import flipnote.group.adapter.out.entity.InviteEntity;
import flipnote.group.adapter.out.entity.RoleEntity;
import flipnote.group.application.port.in.InviteRespondUseCase;
import flipnote.group.application.port.in.command.InviteRespondCommand;
import flipnote.group.application.port.in.result.InviteRespondResult;
import flipnote.group.application.port.out.GroupMemberRepositoryPort;
import flipnote.group.application.port.out.GroupRepositoryPort;
import flipnote.group.application.port.out.GroupRoleRepositoryPort;
import flipnote.group.application.port.out.InviteRepositoryPort;
import flipnote.group.domain.model.invite.InviteStatus;
import flipnote.group.domain.model.member.GroupMemberRole;
import flipnote.group.domain.policy.BusinessException;
import flipnote.group.domain.policy.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InviteRespondService implements InviteRespondUseCase {

	private final InviteRepositoryPort inviteRepository;
	private final GroupRoleRepositoryPort groupRoleRepository;
	private final GroupRepositoryPort groupRepository;
	private final GroupMemberRepositoryPort groupMemberRepository;

	@Override
	@Transactional
	public InviteRespondResult respondToInvite(InviteRespondCommand cmd) {
		// 1. PENDING 초대 찾기 (invitationId + groupId + inviteeUserId)
		InviteEntity invite = inviteRepository.findByIdAndGroupIdAndInviteeUserIdAndStatus(
			cmd.invitationId(), cmd.groupId(), cmd.inviteeUserId(), InviteStatus.PENDING
		);

		// 2. 만료 체크
		invite.validateNotExpired();

		// 3. 상태 변경 (ACCEPTED / REJECTED)
		invite.respond(cmd.status());

		// 4. ACCEPTED시: 그룹 멤버 추가
		if (cmd.status() == InviteStatus.ACCEPTED) {
			// 이미 멤버인 경우 무시
			if (!groupMemberRepository.existsUserInGroup(cmd.groupId(), cmd.inviteeUserId())) {
				boolean joinable = groupRepository.checkJoinable(cmd.groupId());
				if (!joinable) {
					throw new BusinessException(ErrorCode.GROUP_MEMBER_LIMIT_EXCEEDED);
				}

				RoleEntity role = groupRoleRepository.findByGroupIdAndRole(cmd.groupId(), GroupMemberRole.MEMBER);
				GroupMemberEntity groupMember = GroupMemberEntity.create(cmd.groupId(), cmd.inviteeUserId(), role);
				groupMemberRepository.save(groupMember);
			}
		}

		return new InviteRespondResult(invite);
	}
}
