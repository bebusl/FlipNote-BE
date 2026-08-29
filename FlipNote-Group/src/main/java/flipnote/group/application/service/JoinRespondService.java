package flipnote.group.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.group.adapter.out.entity.GroupMemberEntity;
import flipnote.group.adapter.out.entity.JoinEntity;
import flipnote.group.adapter.out.entity.RoleEntity;
import flipnote.group.application.port.in.JoinRespondUseCase;
import flipnote.group.application.port.in.command.JoinRespondCommand;
import flipnote.group.application.port.in.result.JoinRespondResult;
import flipnote.group.application.port.out.GroupMemberRepositoryPort;
import flipnote.group.application.port.out.GroupRepositoryPort;
import flipnote.group.application.port.out.GroupRoleRepositoryPort;
import flipnote.group.application.port.out.JoinRepositoryPort;
import flipnote.group.domain.model.join.JoinStatus;
import flipnote.group.domain.model.member.GroupMemberRole;
import flipnote.group.domain.model.permission.GroupPermission;
import flipnote.group.domain.policy.BusinessException;
import flipnote.group.domain.policy.ErrorCode;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class JoinRespondService implements JoinRespondUseCase {

	private final JoinRepositoryPort joinRepository;
	private final GroupRoleRepositoryPort groupRoleRepository;
	private final GroupRepositoryPort groupRepository;
	private final GroupMemberRepositoryPort groupMemberRepository;

	private static final GroupPermission MANAGE = GroupPermission.JOIN_REQUEST_MANAGE;

	/**
	 * 가입 신청 응답
	 * @param cmd
	 * @return
	 */
	@Override
	@Transactional
	public JoinRespondResult joinRespond(JoinRespondCommand cmd) {

		//권한 체크
		boolean existPermission = groupRoleRepository.checkPermission(cmd.userId(), cmd.groupId(), MANAGE);
		
		if(!existPermission) {
			throw new BusinessException(ErrorCode.PERMISSION_NOT_FOUND);
		}

		JoinEntity join = joinRepository.findJoin(cmd.joinId());

		if(join.getStatus().equals(JoinStatus.ACCEPT)) {
			throw new BusinessException(ErrorCode.JOIN_ALREADY_ACCEPTED);
		}

		join.updateStatus(cmd.status());

		//거절일 경우
		if(cmd.status().equals(JoinStatus.REJECT)) {
			JoinEntity response = joinRepository.updateJoin(join);

			return new JoinRespondResult(response);
		}

		//수락일 경우
		boolean joinable = groupRepository.checkJoinable(cmd.groupId());
		
		//꽉찼을 경우
		if(!joinable) {
			throw new BusinessException(ErrorCode.GROUP_MEMBER_LIMIT_EXCEEDED);
		}
		
		//가입 가능한 경우
		RoleEntity role = groupRoleRepository.findByGroupIdAndRole(cmd.groupId(), GroupMemberRole.MEMBER);

		//주의: cmd의 유저가 아닌 join의 아이디로 해야함
		GroupMemberEntity groupMember = GroupMemberEntity.create(cmd.groupId(), join.getUserId(), role);

		groupMemberRepository.save(groupMember);

		JoinEntity response = joinRepository.updateJoin(join);

		return new JoinRespondResult(response);
	}
}
