package flipnote.user.infrastructure.listener;

import org.springframework.context.event.EventListener;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import flipnote.user.domain.PasswordResetConstants;
import flipnote.user.domain.common.EmailSendException;
import flipnote.user.domain.event.PasswordResetCreateEvent;
import flipnote.user.infrastructure.mail.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class PasswordResetEventListener {
	private final MailService mailService;

	@Async
	@EventListener
	@Retryable(delay = 2000, multiplier = 2.0, maxRetries = 3, includes = EmailSendException.class)
	public void handle(PasswordResetCreateEvent event) {
		mailService.sendPasswordResetLink(event.to(), event.link(), PasswordResetConstants.TOKEN_TTL_MINUTES);
	}
}
