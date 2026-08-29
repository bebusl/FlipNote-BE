package flipnote.notification.interfaces.http;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import flipnote.notification.application.FcmTokenService;
import flipnote.notification.application.NotificationCommandService;
import flipnote.notification.application.NotificationQueryService;
import flipnote.notification.application.dto.result.NotificationResult;
import flipnote.notification.interfaces.http.common.CursorPagingResponse;
import flipnote.notification.interfaces.http.common.HttpHeaders;
import flipnote.notification.interfaces.http.dto.request.NotificationListRequest;
import flipnote.notification.interfaces.http.dto.request.TokenRegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/notifications")
public class NotificationController implements NotificationControllerDocs {

	private final NotificationQueryService notificationQueryService;
	private final NotificationCommandService notificationCommandService;
	private final FcmTokenService fcmTokenService;

	@Override
	@GetMapping
	public ResponseEntity<CursorPagingResponse<NotificationResult>> getNotifications(
		@Valid @ModelAttribute NotificationListRequest req,
		@RequestHeader(HttpHeaders.USER_ID) Long userId
	) {
		CursorPagingResponse<NotificationResult> res = CursorPagingResponse.from(
			notificationQueryService.getNotifications(userId, req.toCommand())
		);

		return ResponseEntity.ok(res);
	}

	@Override
	@PostMapping("/token")
	public ResponseEntity<Void> registerFcmToken(
		@Valid @RequestBody TokenRegisterRequest req,
		@RequestHeader(HttpHeaders.USER_ID) Long userId
	) {
		fcmTokenService.registerFcmToken(userId, req.token());

		return ResponseEntity.status(HttpStatus.CREATED).build();
	}

	@Override
	@PostMapping("/read-all")
	public ResponseEntity<Void> markAllNotificationsAsRead(
		@RequestHeader(HttpHeaders.USER_ID) Long userId
	) {
		notificationCommandService.markAllNotificationsAsRead(userId);

		return ResponseEntity.ok().build();
	}

	@Override
	@PostMapping("/{notificationId}/read")
	public ResponseEntity<Void> markNotificationAsRead(
		@PathVariable("notificationId") Long notificationId,
		@RequestHeader(HttpHeaders.USER_ID) Long userId
	) {
		notificationCommandService.markNotificationAsRead(userId, notificationId);

		return ResponseEntity.ok().build();
	}
}
