package flipnote.user.infrastructure.mail;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Local-development mail transport. It deliberately writes messages only to the
 * application log and must be enabled explicitly with MAIL_MODE=console.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.mail.mode", havingValue = "console")
public class ConsoleMailService implements MailService {

    @Override
    public void sendVerificationCode(String to, String code, int ttl) {
        log.warn("[DEV EMAIL] Verification code for {}: {} (valid for {} minutes)", to, code, ttl);
    }

    @Override
    public void sendPasswordResetLink(String to, String link, int ttl) {
        log.warn("[DEV EMAIL] Password reset link for {}: {} (valid for {} minutes)", to, link, ttl);
    }
}
