package flipnote.notification.domain.fcmtoken;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
	name = "fcm_tokens",
	indexes = {
		@Index(name = "idx_fcm_user_id", columnList = "user_id"),
		@Index(name = "idx_fcm_token", columnList = "token")
	},
	uniqueConstraints = {
		@UniqueConstraint(name = "unique_user_token", columnNames = {"user_id", "token"})
	}
)
public class FcmToken {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long userId;

	@Column(nullable = false, length = 512)
	private String token;

	@Column(nullable = false)
	private LocalDateTime lastUsedAt;

	@Builder
	public FcmToken(Long userId, String token) {
		this.userId = userId;
		this.token = token;
		this.lastUsedAt = LocalDateTime.now();
	}

	public void updateLastUsedAt() {
		this.lastUsedAt = LocalDateTime.now();
	}
}
