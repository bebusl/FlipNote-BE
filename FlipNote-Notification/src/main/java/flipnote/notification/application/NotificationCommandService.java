package flipnote.notification.application;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.text.StringSubstitutor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.notification.application.dto.result.FcmSendResult;
import flipnote.notification.domain.common.BizException;
import flipnote.notification.domain.fcmtoken.FcmToken;
import flipnote.notification.domain.fcmtoken.FcmTokenRepository;
import flipnote.notification.domain.notification.Notification;
import flipnote.notification.domain.notification.NotificationErrorCode;
import flipnote.notification.domain.notification.NotificationRepository;
import flipnote.notification.domain.notification.NotificationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class NotificationCommandService {

	private final NotificationRepository notificationRepository;
	private final FcmTokenRepository fcmTokenRepository;
	private final FcmSender fcmSender;
	private final MessageSource messageSource;

	@Transactional
	public void createGroupInviteNotification(Long groupId, Long inviteeId, String groupName) {
		Notification notification = Notification.builder()
			.receiverId(inviteeId)
			.groupId(groupId)
			.type(NotificationType.GROUP_INVITE)
			.variables(Map.of("groupName", groupName))
			.build();
		notificationRepository.save(notification);

		String message = buildMessage(notification);
		sendFcmNotification(inviteeId, message);
	}

	@Transactional
	public void createGroupJoinRequestNotification(
		Long groupId,
		List<Long> receiverIds,
		Long requesterId,
		String requesterNickname
	) {
		List<Notification> notifications = receiverIds.stream()
			.map(receiverId -> Notification.builder()
				.receiverId(receiverId)
				.groupId(groupId)
				.type(NotificationType.GROUP_JOIN_REQUEST)
				.variables(Map.of("requesterNickname", requesterNickname))
				.metadata(Map.of("requesterId", requesterId))
				.build())
			.toList();
		notificationRepository.saveAll(notifications);

		for (Notification notification : notifications) {
			try {
				String message = buildMessage(notification);
				sendFcmNotification(notification.getReceiverId(), message);
			} catch (Exception ex) {
				log.error(
					"Failed to send group join request notification to receiverId={}, notificationId={}",
					notification.getReceiverId(), notification.getId(), ex
				);
			}
		}
	}

	@Transactional
	public void markNotificationAsRead(Long userId, Long notificationId) {
		Notification notification = notificationRepository.findByIdAndReceiverId(notificationId, userId)
			.orElseThrow(() -> new BizException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

		if (notification.isRead()) {
			throw new BizException(NotificationErrorCode.ALREADY_READ_NOTIFICATION);
		}

		notification.markAsRead();
	}

	@Transactional
	public void markAllNotificationsAsRead(Long userId) {
		notificationRepository.bulkMarkAsRead(userId, LocalDateTime.now());
	}

	private void sendFcmNotification(Long userId, String body) {
		List<FcmToken> fcmTokens = fcmTokenRepository.findByUserId(userId);
		if (fcmTokens.isEmpty()) {
			log.warn("No FCM tokens for user {}", userId);
			return;
		}

		List<String> tokens = fcmTokens.stream().map(FcmToken::getToken).toList();
		FcmSendResult result = fcmSender.sendEachForMulticast(tokens, "알림", body);

		if (!result.invalidTokens().isEmpty()) {
			fcmTokenRepository.deleteByUserIdAndTokenIn(userId, result.invalidTokens());
		}
		if (!result.validTokens().isEmpty()) {
			fcmTokenRepository.bulkUpdateLastUsedAt(result.validTokens(), LocalDateTime.now());
		}
	}

	private String buildMessage(Notification notification) {
		String key = notification.getType().getMessageKey();
		String template = messageSource.getMessage(key, null, key, Locale.KOREA);
		StringSubstitutor substitutor = new StringSubstitutor(notification.getVariables());
		return substitutor.replace(template);
	}
}
