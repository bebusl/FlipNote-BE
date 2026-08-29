package flipnote.notification.application;

import java.util.List;

import flipnote.notification.application.dto.result.FcmSendResult;

public interface FcmSender {

	FcmSendResult sendEachForMulticast(List<String> tokens, String title, String body);
}
