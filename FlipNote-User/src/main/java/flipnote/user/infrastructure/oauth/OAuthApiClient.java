package flipnote.user.infrastructure.oauth;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import flipnote.user.domain.AuthErrorCode;
import flipnote.user.domain.common.BizException;
import lombok.RequiredArgsConstructor;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class OAuthApiClient {

	private final RestClient restClient;
	private final ObjectMapper objectMapper;
	private final OAuthProperties oAuthProperties;

	public String requestAccessToken(
		OAuthProperties.Provider provider,
		String code,
		String codeVerifier
	) {
		MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
		params.add("grant_type", "authorization_code");
		params.add("client_id", provider.getClientId());
		params.add("client_secret", provider.getClientSecret());
		params.add("redirect_uri", buildRedirectUri(provider.getRedirectUri()));
		params.add("code", code);
		params.add("code_verifier", codeVerifier);

		try {
			String responseBody = restClient.post()
				.uri(provider.getTokenUri())
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.body(params)
				.retrieve()
				.body(String.class);

			Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<>() {
			});
			return (String)responseMap.get("access_token");
		} catch (Exception e) {
			throw new BizException(AuthErrorCode.OAUTH_COMMUNICATION_ERROR);
		}
	}

	public Map<String, Object> requestUserInfo(OAuthProperties.Provider provider, String accessToken) {
		try {
			String responseBody = restClient.get()
				.uri(provider.getUserInfoUri())
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
				.retrieve()
				.body(String.class);

			return objectMapper.readValue(responseBody, new TypeReference<>() {
			});
		} catch (Exception e) {
			throw new BizException(AuthErrorCode.OAUTH_COMMUNICATION_ERROR);
		}
	}

	public OAuth2UserInfo createUserInfo(String providerName, Map<String, Object> attributes) {
		return switch (providerName.toLowerCase()) {
			case "google" -> new GoogleUserInfo(attributes);
			default -> throw new BizException(AuthErrorCode.INVALID_OAUTH_PROVIDER);
		};
	}

	public String buildAuthorizeUri(OAuthProperties.Provider provider,
		String codeChallenge, String state) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(provider.getAuthorizationUri())
			.queryParam("client_id", provider.getClientId())
			.queryParam("redirect_uri", buildRedirectUri(provider.getRedirectUri()))
			.queryParam("response_type", "code")
			.queryParam("scope", String.join(" ", provider.getScope()))
			.queryParam("code_challenge", codeChallenge)
			.queryParam("code_challenge_method", "S256");

		if (state != null) {
			builder.queryParam("state", state);
		}

		return builder.toUriString();
	}

	private String buildRedirectUri(String path) {
		return UriComponentsBuilder.fromUriString(oAuthProperties.getBaseUrl())
			.replacePath(path)
			.build()
			.toUriString();
	}
}
