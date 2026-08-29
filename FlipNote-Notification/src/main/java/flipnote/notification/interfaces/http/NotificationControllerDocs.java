package flipnote.notification.interfaces.http;

import org.springframework.http.ResponseEntity;

import flipnote.notification.application.dto.result.NotificationResult;
import flipnote.notification.interfaces.http.common.CursorPagingResponse;
import flipnote.notification.interfaces.http.dto.request.NotificationListRequest;
import flipnote.notification.interfaces.http.dto.request.TokenRegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Notification", description = "Notification API")
public interface NotificationControllerDocs {

	@Operation(summary = "알림 목록 조회")
	ResponseEntity<CursorPagingResponse<NotificationResult>> getNotifications(
		NotificationListRequest req,
		Long userId
	);

	@Operation(summary = "FCM 토큰 등록")
	ResponseEntity<Void> registerFcmToken(TokenRegisterRequest req, Long userId);

	@Operation(summary = "모든 알림 읽음 처리")
	ResponseEntity<Void> markAllNotificationsAsRead(Long userId);

	@Operation(summary = "알림 읽음 처리")
	ResponseEntity<Void> markNotificationAsRead(Long notificationId, Long userId);
}
