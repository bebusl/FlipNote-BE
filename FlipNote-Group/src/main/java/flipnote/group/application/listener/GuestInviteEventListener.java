package flipnote.group.application.listener;

import org.springframework.context.event.EventListener;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import flipnote.group.domain.event.GuestInviteCreatedEvent;
import flipnote.group.global.config.ClientProperties;
import flipnote.group.infrastructure.email.EmailSendException;
import flipnote.group.infrastructure.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class GuestInviteEventListener {

	private final EmailService emailService;
	private final ClientProperties clientProperties;

	@Async
	@EventListener
	@Retryable(delay = 2000, multiplier = 2.0, maxRetries = 3, includes = EmailSendException.class)
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleGuestInviteCreatedEvent(GuestInviteCreatedEvent event) {
		emailService.sendGuestGroupInvitation(event.email(), event.groupName(), clientProperties.getUrl());
	}
}
