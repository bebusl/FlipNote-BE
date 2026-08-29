package flipnote.user.infrastructure.oauth;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Validated
@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.oauth2")
public class OAuthProperties {

	@NotBlank
	private final String baseUrl;

	@Valid
	private final Map<String, Provider> providers;

	@Getter
	@Setter
	public static class Provider {
		@NotBlank
		private String clientId;
		@NotBlank
		private String clientSecret;
		@NotBlank
		private String redirectUri;
		@NotBlank
		private String authorizationUri;
		@NotBlank
		private String tokenUri;
		@NotBlank
		private String userInfoUri;
		@NotEmpty
		private List<String> scope;
	}
}
