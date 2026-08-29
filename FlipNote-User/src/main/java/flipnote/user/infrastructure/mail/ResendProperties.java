package flipnote.user.infrastructure.mail;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.resend")
public class ResendProperties {

    @NotEmpty
    private final String apiKey;

    @NotEmpty
    private final String fromEmail;
}
