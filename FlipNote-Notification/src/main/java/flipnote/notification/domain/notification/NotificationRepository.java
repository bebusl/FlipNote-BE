package flipnote.notification.domain.notification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

	@Query("""
		SELECT n FROM Notification n
		WHERE (:cursor IS NULL OR n.id < :cursor)
		AND (:groupId IS NULL OR n.groupId = :groupId)
		AND (:read IS NULL OR n.read = :read)
		AND n.receiverId = :receiverId
		""")
	List<Notification> findNotificationsByReceiverIdAndCursor(
		@Param("receiverId") Long receiverId,
		@Param("cursor") Long cursor,
		@Param("groupId") Long groupId,
		@Param("read") Boolean read,
		Pageable pageable
	);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
		UPDATE Notification n
		SET n.read = TRUE, n.readAt = :now
		WHERE n.receiverId = :userId
		AND n.read is FALSE
		""")
	int bulkMarkAsRead(
		@Param("userId") Long userId,
		@Param("now") LocalDateTime now
	);

	Optional<Notification> findByIdAndReceiverId(Long id, Long receiverId);
}
