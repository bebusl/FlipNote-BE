package flipnote.notification.domain.notification;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import flipnote.notification.domain.common.BaseEntity;
import flipnote.notification.infrastructure.persistence.MapToJsonConverter;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "notifications")
public class Notification extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private Long receiverId;

	private Long groupId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private NotificationType type;

	@Convert(converter = MapToJsonConverter.class)
	private Map<String, Object> variables;

	@Convert(converter = MapToJsonConverter.class)
	private Map<String, Object> metadata;

	@Column(name = "is_read", nullable = false)
	private boolean read;

	private LocalDateTime readAt;

	@Builder
	public Notification(
		Long receiverId,
		Long groupId,
		NotificationType type,
		Map<String, Object> variables,
		Map<String, Object> metadata
	) {
		this.receiverId = receiverId;
		this.groupId = groupId;
		this.type = type;
		this.variables = variables == null ? new HashMap<>() : variables;
		this.metadata = metadata == null ? new HashMap<>() : metadata;
		this.read = false;
	}

	public void markAsRead() {
		if (!this.read) {
			this.read = true;
			this.readAt = LocalDateTime.now();
		}
	}
}
