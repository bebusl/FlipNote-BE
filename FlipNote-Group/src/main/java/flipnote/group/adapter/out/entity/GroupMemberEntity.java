package flipnote.group.adapter.out.entity;

import flipnote.group.domain.model.BaseEntity;
import flipnote.group.domain.model.member.GroupMemberRole;
import flipnote.group.domain.model.member.MemberInfo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
	name = "group_members",
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uk_group_members_group_user",
			columnNames = {"group_id", "user_id"}
		)
	}
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupMemberEntity extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "group_id", nullable = false)
	private Long groupId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "group_role_id", nullable = false)
	private RoleEntity role;

	@Builder
	private GroupMemberEntity(Long id, Long groupId, Long userId, RoleEntity role) {
		this.id = id;
		this.groupId = groupId;
		this.userId = userId;
		this.role = role;
	}

	/**
	 * 멤버 생성
	 * @param groupId
	 * @param userId
	 * @return
	 */
	public static GroupMemberEntity create(Long groupId, Long userId, RoleEntity role) {
		return GroupMemberEntity.builder()
			.groupId(groupId)
			.userId(userId)
			.role(role)
			.build();
	}

	public MemberInfo toMemberInfo() {
		return MemberInfo.builder()
			.memberId(this.getId())
			.userId(this.getUserId())
			.role(this.getRole().getRole())
			.build();
	}

	public void changeRole(RoleEntity role) {
		this.role = role;
	}
}
