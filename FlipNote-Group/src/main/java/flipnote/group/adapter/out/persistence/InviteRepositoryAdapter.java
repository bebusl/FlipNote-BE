package flipnote.group.adapter.out.persistence;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import flipnote.group.adapter.out.entity.InviteEntity;
import flipnote.group.application.port.out.InviteRepositoryPort;
import flipnote.group.domain.model.invite.InviteStatus;
import flipnote.group.domain.policy.BusinessException;
import flipnote.group.domain.policy.ErrorCode;
import flipnote.group.infrastructure.persistence.jpa.InviteJpaRepository;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class InviteRepositoryAdapter implements InviteRepositoryPort {

	private final InviteJpaRepository inviteJpaRepository;

	@Override
	public InviteEntity save(InviteEntity invite) {
		return inviteJpaRepository.save(invite);
	}

	@Override
	public void delete(InviteEntity invite) {
		inviteJpaRepository.delete(invite);
	}

	@Override
	public InviteEntity findByIdAndStatus(Long id, InviteStatus status) {
		return inviteJpaRepository.findByIdAndStatus(id, status)
			.orElseThrow(() -> new BusinessException(ErrorCode.INVITE_NOT_FOUND));
	}

	@Override
	public InviteEntity findByIdAndGroupIdAndInviteeUserIdAndStatus(Long id, Long groupId, Long inviteeUserId, InviteStatus status) {
		return inviteJpaRepository.findByIdAndGroupIdAndInviteeUserIdAndStatus(id, groupId, inviteeUserId, status)
			.orElseThrow(() -> new BusinessException(ErrorCode.INVITE_NOT_FOUND));
	}

	@Override
	public List<InviteEntity> findAllByGroupId(Long groupId) {
		return inviteJpaRepository.findAllByGroupId(groupId);
	}

	@Override
	public Page<InviteEntity> findAllByGroupId(Long groupId, Pageable pageable) {
		return inviteJpaRepository.findAllByGroupId(groupId, pageable);
	}

	@Override
	public List<InviteEntity> findAllByInviteeUserId(Long inviteeUserId) {
		return inviteJpaRepository.findAllByInviteeUserId(inviteeUserId);
	}

	@Override
	public Page<InviteEntity> findAllByInviteeUserId(Long inviteeUserId, Pageable pageable) {
		return inviteJpaRepository.findAllByInviteeUserId(inviteeUserId, pageable);
	}

	@Override
	public boolean existsByGroupIdAndInviteeUserIdAndStatus(Long groupId, Long inviteeUserId, InviteStatus status) {
		return inviteJpaRepository.existsByGroupIdAndInviteeUserIdAndStatus(groupId, inviteeUserId, status);
	}

	@Override
	public boolean existsByGroupIdAndInviteeEmailAndStatus(Long groupId, String inviteeEmail, InviteStatus status) {
		return inviteJpaRepository.existsByGroupIdAndInviteeEmailAndStatus(groupId, inviteeEmail, status);
	}

	@Override
	public void bulkUpdateInviteeUserId(String inviteeEmail, Long inviteeUserId) {
		inviteJpaRepository.bulkUpdateInviteeUserId(inviteeEmail, inviteeUserId);
	}
}
