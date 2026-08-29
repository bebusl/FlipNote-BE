package flipnote.group.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.resend.Resend;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Configuration
public class ResendConfig {

	private final ResendProperties resendProperties;

	@Bean
	public Resend resend() {
		return new Resend(resendProperties.getApiKey());
	}
}
