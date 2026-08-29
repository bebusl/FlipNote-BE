package flipnote.group.adapter.out.entity;

import flipnote.group.domain.model.member.GroupMemberRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "group_roles",
	uniqueConstraints = @UniqueConstraint(
		name = "uk_group_roles_group_role",
		columnNames = {"group_id", "group_role"}
	)
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoleEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "group_id", nullable = false)
	private Long groupId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 50, name = "group_role")
	private GroupMemberRole role;

	@Builder
	private RoleEntity(Long groupId, GroupMemberRole role) {
		this.groupId = groupId;
		this.role = role;
	}

	public static RoleEntity create(Long groupId, GroupMemberRole groupMemberRole) {
		return RoleEntity.builder()
			.groupId(groupId)
			.role(groupMemberRole)
			.build();
	}
}

