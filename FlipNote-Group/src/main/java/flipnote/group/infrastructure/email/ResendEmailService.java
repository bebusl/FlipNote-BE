package flipnote.group.infrastructure.email;

import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;

import flipnote.group.global.config.ResendProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class ResendEmailService implements EmailService {

	private final ResendProperties resendProperties;
	private final Resend resend;
	private final SpringTemplateEngine templateEngine;

	@Override
	public void sendGuestGroupInvitation(String to, String groupName, String registerUrl) {
		Context context = new Context();
		context.setVariable("groupName", groupName);
		context.setVariable("registerUrl", registerUrl);

		String html = templateEngine.process("email/guest-group-invitation", context);

		CreateEmailOptions params = CreateEmailOptions.builder()
			.from(resendProperties.getFromEmail())
			.to(to)
			.subject("그룹 초대 안내")
			.html(html)
			.build();

		try {
			resend.emails().send(params);
		} catch (ResendException e) {
			log.error("비회원 그룹 초대 발송 실패: email={}", to, e);
			throw new EmailSendException(e);
		}
	}
}
