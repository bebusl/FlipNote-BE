package flipnote.group.adapter.out.entity;

import java.time.LocalDateTime;

import flipnote.group.domain.model.BaseEntity;
import flipnote.group.domain.model.invite.InviteStatus;
import flipnote.group.domain.policy.BusinessException;
import flipnote.group.domain.policy.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "invitations",
	indexes = {
		@Index(name = "idx_invite_group_invitee_user", columnList = "group_id, invitee_user_id, status"),
		@Index(name = "idx_invite_group_invitee_email", columnList = "group_id, invitee_email, status"),
		@Index(name = "idx_invite_invitee_user_status", columnList = "invitee_user_id, status")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "uq_invite_group_invitee_user", columnNames = {"group_id", "invitee_user_id"}),
		@UniqueConstraint(name = "uq_invite_group_invitee_email", columnNames = {"group_id", "invitee_email"})
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InviteEntity extends BaseEntity {

	private static final long DEFAULT_EXPIRATION_DAYS = 7L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "group_id", nullable = false)
	private Long groupId;

	@Column(name = "inviter_user_id", nullable = false)
	private Long inviterUserId;

	@Column(name = "invitee_user_id")
	private Long inviteeUserId;

	@Column(name = "invitee_email")
	private String inviteeEmail;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private InviteStatus status;

	@Column(name = "expired_at", nullable = false)
	private LocalDateTime expiredAt;

	@Builder
	private InviteEntity(Long id, Long groupId, Long inviterUserId, Long inviteeUserId,
		String inviteeEmail, InviteStatus status, LocalDateTime expiredAt) {
		this.id = id;
		this.groupId = groupId;
		this.inviterUserId = inviterUserId;
		this.inviteeUserId = inviteeUserId;
		this.inviteeEmail = inviteeEmail;
		this.status = status;
		this.expiredAt = expiredAt;
	}

	public static InviteEntity create(Long groupId, Long inviterUserId, Long inviteeUserId, String inviteeEmail) {
		return InviteEntity.builder()
			.groupId(groupId)
			.inviterUserId(inviterUserId)
			.inviteeUserId(inviteeUserId)
			.inviteeEmail(inviteeEmail)
			.status(InviteStatus.PENDING)
			.expiredAt(LocalDateTime.now().plusDays(DEFAULT_EXPIRATION_DAYS))
			.build();
	}

	public void respond(InviteStatus status) {
		this.status = status;
	}

	public void validateNotExpired() {
		if (isExpired()) {
			throw new BusinessException(ErrorCode.INVITE_NOT_FOUND);
		}
	}

	public boolean isExpired() {
		if (this.status == InviteStatus.EXPIRED) {
			return true;
		}
		if (this.status != InviteStatus.PENDING) {
			return false;
		}
		return this.expiredAt.isBefore(LocalDateTime.now());
	}

	public InviteStatus getStatus() {
		if (isExpired()) {
			return InviteStatus.EXPIRED;
		}
		return status;
	}
}
