package flipnote.group.application.port.out;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import flipnote.group.adapter.out.entity.InviteEntity;
import flipnote.group.domain.model.invite.InviteStatus;

public interface InviteRepositoryPort {

	InviteEntity save(InviteEntity invite);

	void delete(InviteEntity invite);

	InviteEntity findByIdAndStatus(Long id, InviteStatus status);

	InviteEntity findByIdAndGroupIdAndInviteeUserIdAndStatus(Long id, Long groupId, Long inviteeUserId, InviteStatus status);

	List<InviteEntity> findAllByGroupId(Long groupId);

	Page<InviteEntity> findAllByGroupId(Long groupId, Pageable pageable);

	List<InviteEntity> findAllByInviteeUserId(Long inviteeUserId);

	Page<InviteEntity> findAllByInviteeUserId(Long inviteeUserId, Pageable pageable);

	boolean existsByGroupIdAndInviteeUserIdAndStatus(Long groupId, Long inviteeUserId, InviteStatus status);

	boolean existsByGroupIdAndInviteeEmailAndStatus(Long groupId, String inviteeEmail, InviteStatus status);

	void bulkUpdateInviteeUserId(String inviteeEmail, Long inviteeUserId);
}
