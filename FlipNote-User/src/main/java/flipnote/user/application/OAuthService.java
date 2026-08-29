package flipnote.user.application;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import flipnote.user.domain.AuthErrorCode;
import flipnote.user.domain.TokenPair;
import flipnote.user.domain.UserErrorCode;
import flipnote.user.domain.common.BizException;
import flipnote.user.domain.entity.OAuthLink;
import flipnote.user.domain.entity.User;
import flipnote.user.domain.repository.OAuthLinkRepository;
import flipnote.user.domain.repository.UserRepository;
import flipnote.user.infrastructure.jwt.JwtProvider;
import flipnote.user.infrastructure.oauth.OAuth2UserInfo;
import flipnote.user.infrastructure.oauth.OAuthApiClient;
import flipnote.user.infrastructure.oauth.OAuthProperties;
import flipnote.user.infrastructure.oauth.PkceUtil;
import flipnote.user.infrastructure.redis.SocialLinkTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OAuthService {

	private final PkceUtil pkceUtil;
	private final OAuthApiClient oAuthApiClient;
	private final OAuthLinkRepository oAuthLinkRepository;
	private final UserRepository userRepository;
	private final SocialLinkTokenRepository socialLinkTokenRepository;
	private final JwtProvider jwtProvider;
	private final OAuthProperties oAuthProperties;

	public record AuthorizationRedirect(String authorizeUri, String codeVerifier) {
	}

	public AuthorizationRedirect getAuthorizationUri(String providerName, Long userId) {
		OAuthProperties.Provider provider = resolveProvider(providerName);

		String codeVerifier = pkceUtil.generateCodeVerifier();
		String codeChallenge = pkceUtil.generateCodeChallenge(codeVerifier);

		String state = null;
		if (userId != null) {
			state = UUID.randomUUID().toString();
			socialLinkTokenRepository.save(userId, state);
		}

		String authorizeUri = oAuthApiClient.buildAuthorizeUri(provider, codeChallenge, state);

		return new AuthorizationRedirect(authorizeUri, codeVerifier);
	}

	public TokenPair socialLogin(String providerName, String code, String codeVerifier) {
		OAuth2UserInfo userInfo = getOAuth2UserInfo(providerName, code, codeVerifier);

		OAuthLink oAuthLink = oAuthLinkRepository
			.findByProviderAndProviderIdWithUser(userInfo.getProvider(), userInfo.getProviderId())
			.orElseThrow(() -> new BizException(AuthErrorCode.NOT_REGISTERED_SOCIAL_ACCOUNT));

		return jwtProvider.generateTokenPair(oAuthLink.getUser());
	}

	@Transactional
	public void linkSocialAccount(String providerName, String code, String state, String codeVerifier) {
		Long userId = socialLinkTokenRepository.findUserIdByState(state)
			.orElseThrow(() -> new BizException(AuthErrorCode.INVALID_SOCIAL_LINK_TOKEN));

		socialLinkTokenRepository.delete(state);

		OAuth2UserInfo userInfo = getOAuth2UserInfo(providerName, code, codeVerifier);

		if (oAuthLinkRepository.existsByProviderAndProviderId(userInfo.getProvider(), userInfo.getProviderId())) {
			throw new BizException(AuthErrorCode.ALREADY_LINKED_SOCIAL_ACCOUNT);
		}

		User user = userRepository.findByIdAndStatus(userId, User.Status.ACTIVE)
			.orElseThrow(() -> new BizException(UserErrorCode.USER_NOT_FOUND));

		OAuthLink link = OAuthLink.builder()
			.provider(userInfo.getProvider())
			.providerId(userInfo.getProviderId())
			.user(user)
			.build();
		oAuthLinkRepository.save(link);
	}

	private OAuth2UserInfo getOAuth2UserInfo(String providerName, String code, String codeVerifier) {
		OAuthProperties.Provider provider = resolveProvider(providerName);
		String accessToken = oAuthApiClient.requestAccessToken(provider, code, codeVerifier);
		Map<String, Object> attributes = oAuthApiClient.requestUserInfo(provider, accessToken);
		return oAuthApiClient.createUserInfo(providerName, attributes);
	}

	private OAuthProperties.Provider resolveProvider(String providerName) {
		Map<String, OAuthProperties.Provider> providers = oAuthProperties.getProviders();
		if (providers == null) {
			throw new BizException(AuthErrorCode.INVALID_OAUTH_PROVIDER);
		}
		OAuthProperties.Provider provider = providers.get(providerName.toLowerCase());
		if (provider == null) {
			log.warn("지원하지 않는 OAuth Provider: {}", providerName);
			throw new BizException(AuthErrorCode.INVALID_OAUTH_PROVIDER);
		}
		return provider;
	}
}
