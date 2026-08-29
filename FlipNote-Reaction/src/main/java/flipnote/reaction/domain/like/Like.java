package flipnote.reaction.domain.like;

import flipnote.reaction.domain.common.BaseEntity;
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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "likes",
	uniqueConstraints = {
		@UniqueConstraint(name = "uk_like_target_user", columnNames = {"targetType", "targetId", "userId"})
	},
	indexes = {
		@Index(name = "idx_like_user_target_type", columnList = "userId, targetType")
	}
)
public class Like extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private LikeTargetType targetType;

	@Column(nullable = false)
	private Long targetId;

	@Column(nullable = false)
	private Long userId;

	@Builder
	public Like(LikeTargetType targetType, Long targetId, Long userId) {
		this.targetType = targetType;
		this.targetId = targetId;
		this.userId = userId;
	}
}
