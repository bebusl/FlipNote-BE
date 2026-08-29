package flipnote.notification.infrastructure.fcm;

public enum FcmErrorCode {
	INVALID_ARGUMENT,
	UNREGISTERED,
	SENDER_ID_MISMATCH,
	QUOTA_EXCEEDED,
	DEVICE_MESSAGE_RATE_EXCEEDED,
	TOPIC_MESSAGE_RATE_EXCEEDED,
	UNAVAILABLE,
	INTERNAL,
	UNKNOWN;

	public static FcmErrorCode from(String code) {
		try {
			return FcmErrorCode.valueOf(code);
		} catch (Exception e) {
			return UNKNOWN;
		}
	}
}
