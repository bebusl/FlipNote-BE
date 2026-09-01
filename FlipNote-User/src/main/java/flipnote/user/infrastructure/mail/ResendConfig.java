package flipnote.user.infrastructure.mail;

import com.resend.Resend;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.mail.mode", havingValue = "resend", matchIfMissing = true)
public class ResendConfig {

    private final ResendProperties resendProperties;

    @Bean
    public Resend resend() {
        return new Resend(resendProperties.getApiKey());
    }
}
