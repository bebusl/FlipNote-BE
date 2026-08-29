package flipnote.notification.infrastructure.fcm;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;

import flipnote.notification.application.FcmSender;
import flipnote.notification.application.dto.result.FcmSendResult;
import flipnote.notification.domain.common.BizException;
import flipnote.notification.domain.notification.NotificationErrorCode;

@Component
public class FirebaseFcmSender implements FcmSender {

	@Override
	public FcmSendResult sendEachForMulticast(List<String> tokens, String title, String body) {
		Notification notification = Notification.builder()
			.setTitle(title)
			.setBody(body)
			.build();
		MulticastMessage message = MulticastMessage.builder()
			.addAllTokens(tokens)
			.setNotification(notification)
			.build();

		try {
			BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message);
			return toSendResult(tokens, response);
		} catch (FirebaseMessagingException e) {
			String errorName = e.getMessagingErrorCode() != null ? e.getMessagingErrorCode().name() : "INTERNAL";
			FcmErrorCode code = FcmErrorCode.from(errorName);
			if (code == FcmErrorCode.UNAVAILABLE) {
				throw new BizException(NotificationErrorCode.FCM_SERVER_UNAVAILABLE);
			}
			throw new BizException(NotificationErrorCode.FCM_INTERNAL_ERROR);
		}
	}

	private FcmSendResult toSendResult(List<String> tokens, BatchResponse response) {
		List<String> validTokens = new ArrayList<>();
		List<String> invalidTokens = new ArrayList<>();

		for (int i = 0; i < response.getResponses().size(); i++) {
			SendResponse res = response.getResponses().get(i);
			if (res.isSuccessful()) {
				validTokens.add(tokens.get(i));
			} else {
				String errorName = res.getException().getMessagingErrorCode().name();
				FcmErrorCode code = FcmErrorCode.from(errorName);
				if (code == FcmErrorCode.UNREGISTERED || code == FcmErrorCode.INVALID_ARGUMENT) {
					invalidTokens.add(tokens.get(i));
				}
			}
		}

		return new FcmSendResult(validTokens, invalidTokens);
	}
}
