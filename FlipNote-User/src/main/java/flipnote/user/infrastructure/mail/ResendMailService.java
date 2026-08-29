package flipnote.user.infrastructure.mail;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import flipnote.user.domain.common.EmailSendException;
import flipnote.user.infrastructure.mail.ResendProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResendMailService implements MailService {

    private final ResendProperties resendProperties;
    private final Resend resend;
    private final SpringTemplateEngine templateEngine;

    @Override
    public void sendVerificationCode(String to, String code, int ttl) {
        Context context = new Context();
        context.setVariable("code", code);
        context.setVariable("validMinutes", ttl);

        String html = templateEngine.process("email/email-verification", context);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(resendProperties.getFromEmail())
                .to(to)
                .subject("이메일 인증번호 안내")
                .html(html)
                .build();

        try {
            resend.emails().send(params);
        } catch (ResendException e) {
            log.error("이메일 인증번호 발송 실패: to={}, ttl={}분", to, ttl, e);
            throw new EmailSendException(e);
        }
    }

    @Override
    public void sendPasswordResetLink(String to, String link, int ttl) {
        Context context = new Context();
        context.setVariable("link", link);
        context.setVariable("validMinutes", ttl);

        String html = templateEngine.process("email/password-reset", context);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from(resendProperties.getFromEmail())
                .to(to)
                .subject("비밀번호 재설정 안내")
                .html(html)
                .build();

        try {
            resend.emails().send(params);
        } catch (ResendException e) {
            log.error("비밀번호 재설정 링크 발송 실패: to={}, ttl={}분", to, ttl, e);
            throw new EmailSendException(e);
        }
    }
}
