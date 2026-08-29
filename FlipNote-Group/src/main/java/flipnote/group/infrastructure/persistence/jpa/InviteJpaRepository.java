package flipnote.group.infrastructure.persistence.jpa;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import flipnote.group.adapter.out.entity.InviteEntity;
import flipnote.group.domain.model.invite.InviteStatus;

public interface InviteJpaRepository extends JpaRepository<InviteEntity, Long> {

	Optional<InviteEntity> findByIdAndStatus(Long id, InviteStatus status);

	Optional<InviteEntity> findByIdAndGroupIdAndInviteeUserIdAndStatus(
		Long id, Long groupId, Long inviteeUserId, InviteStatus status
	);

	List<InviteEntity> findAllByGroupId(Long groupId);

	Page<InviteEntity> findAllByGroupId(Long groupId, Pageable pageable);

	List<InviteEntity> findAllByInviteeUserId(Long inviteeUserId);

	Page<InviteEntity> findAllByInviteeUserId(Long inviteeUserId, Pageable pageable);

	boolean existsByGroupIdAndInviteeUserIdAndStatus(Long groupId, Long inviteeUserId, InviteStatus status);

	boolean existsByGroupIdAndInviteeEmailAndStatus(Long groupId, String inviteeEmail, InviteStatus status);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		UPDATE InviteEntity i
		SET i.inviteeUserId = :inviteeUserId
		WHERE i.inviteeEmail = :inviteeEmail
		""")
	int bulkUpdateInviteeUserId(@Param("inviteeEmail") String inviteeEmail, @Param("inviteeUserId") Long inviteeUserId);
}
