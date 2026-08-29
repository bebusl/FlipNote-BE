package flipnote.notification.application.dto.result;

import java.util.List;

public record FcmSendResult(
	List<String> validTokens,
	List<String> invalidTokens
) {
}
