package flipnote.user.interfaces.http;

import flipnote.user.application.OAuthService;
import flipnote.user.domain.AuthErrorCode;
import flipnote.user.domain.TokenPair;
import flipnote.user.domain.common.BizException;
import flipnote.user.infrastructure.config.ClientProperties;
import flipnote.user.infrastructure.jwt.JwtProvider;
import flipnote.user.interfaces.http.common.CookieUtil;
import flipnote.user.interfaces.http.common.HttpConstants;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Slf4j
@RestController
@RequiredArgsConstructor
public class OAuthController {

    private final OAuthService oAuthService;
    private final JwtProvider jwtProvider;
    private final ClientProperties clientProperties;

    private static final int VERIFIER_COOKIE_MAX_AGE = 180;

    @GetMapping("/oauth2/authorization/{provider}")
    public ResponseEntity<Void> redirectToProvider(
            @PathVariable String provider,
            @RequestHeader(value = HttpConstants.USER_ID_HEADER, required = false) Long userId) {
        OAuthService.AuthorizationRedirect redirect = oAuthService.getAuthorizationUri(provider, userId);

        ResponseCookie verifierCookie = ResponseCookie.from(HttpConstants.OAUTH_VERIFIER_COOKIE, redirect.codeVerifier())
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(VERIFIER_COOKIE_MAX_AGE)
                .sameSite("Lax")
                .build();

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.SET_COOKIE, verifierCookie.toString())
                .location(URI.create(redirect.authorizeUri()))
                .build();
    }

    @GetMapping("/oauth2/callback/{provider}")
    public ResponseEntity<Void> handleCallback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam(required = false) String state,
            @CookieValue(HttpConstants.OAUTH_VERIFIER_COOKIE) String codeVerifier,
            HttpServletResponse response) {

        CookieUtil.deleteCookie(response, HttpConstants.OAUTH_VERIFIER_COOKIE);

        boolean isSocialLinkRequest = StringUtils.hasText(state);
        if (isSocialLinkRequest) {
            return handleSocialLink(provider, code, state, codeVerifier);
        }
        return handleSocialLogin(provider, code, codeVerifier, response);
    }

    private ResponseEntity<Void> handleSocialLogin(String provider, String code, String codeVerifier,
                                                    HttpServletResponse response) {
        try {
            TokenPair tokenPair = oAuthService.socialLogin(provider, code, codeVerifier);
            CookieUtil.addCookie(response, HttpConstants.ACCESS_TOKEN_COOKIE, tokenPair.accessToken(),
                    jwtProvider.getAccessTokenExpiration() / 1000);
            CookieUtil.addCookie(response, HttpConstants.REFRESH_TOKEN_COOKIE, tokenPair.refreshToken(),
                    jwtProvider.getRefreshTokenExpiration() / 1000);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(clientProperties.getUrl() + clientProperties.getPaths().getSocialLoginSuccess()))
                    .build();
        } catch (Exception e) {
            log.warn("소셜 로그인 처리 실패. provider: {}", provider, e);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(clientProperties.getUrl() + clientProperties.getPaths().getSocialLoginFailure()))
                    .build();
        }
    }

    private ResponseEntity<Void> handleSocialLink(String provider, String code, String state, String codeVerifier) {
        try {
            oAuthService.linkSocialAccount(provider, code, state, codeVerifier);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(clientProperties.getUrl() + clientProperties.getPaths().getSocialLinkSuccess()))
                    .build();
        } catch (BizException e) {
            log.warn("소셜 계정 연동 처리 실패. provider: {}", provider, e);
            if (e.getErrorCode() == AuthErrorCode.ALREADY_LINKED_SOCIAL_ACCOUNT) {
                return ResponseEntity.status(HttpStatus.FOUND)
                        .location(URI.create(clientProperties.getUrl() + clientProperties.getPaths().getSocialLinkConflict()))
                        .build();
            }
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(clientProperties.getUrl() + clientProperties.getPaths().getSocialLinkFailure()))
                    .build();
        }
    }
}
