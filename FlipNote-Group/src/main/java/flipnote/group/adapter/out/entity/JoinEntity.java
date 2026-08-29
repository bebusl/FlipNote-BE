package flipnote.group.adapter.out.entity;

import flipnote.group.domain.model.BaseEntity;
import flipnote.group.domain.model.join.JoinStatus;
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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "joins")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JoinEntity extends BaseEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "group_id", nullable = false)
	private Long groupId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private JoinStatus status;

	@Column(name = "form")
	private String form;

	@Builder
	private JoinEntity(Long id, Long userId, Long groupId, JoinStatus status, String form) {
		this.id = id;
		this.userId = userId;
		this.groupId = groupId;
		this.status = status;
		this.form = form;
	}

	public static JoinEntity create(Long groupId, Long userId, String form, JoinStatus status) {
		return JoinEntity.builder()
			.groupId(groupId)
			.userId(userId)
			.form(form)
			.status(status)
			.build();
	}

	public void updateStatus(JoinStatus status) {
		this.status = status;
	}
}
