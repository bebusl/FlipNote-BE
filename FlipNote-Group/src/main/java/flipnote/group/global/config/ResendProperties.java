package flipnote.group.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Validated
@ConfigurationProperties("app.resend")
@Component
public class ResendProperties {

	@NotEmpty
	private String fromEmail;

	@NotEmpty
	private String apiKey;
}
