package flipnote.user.interfaces.http;

import flipnote.user.application.AuthService;
import flipnote.user.application.result.SocialLinksResult;
import flipnote.user.application.result.TokenValidateResult;
import flipnote.user.application.result.UserRegisterResult;
import flipnote.user.domain.TokenPair;
import flipnote.user.infrastructure.jwt.JwtProvider;
import flipnote.user.interfaces.http.common.CookieUtil;
import flipnote.user.interfaces.http.common.HttpConstants;
import flipnote.user.interfaces.http.dto.request.ChangePasswordRequest;
import flipnote.user.interfaces.http.dto.request.EmailVerificationRequest;
import flipnote.user.interfaces.http.dto.request.EmailVerifyRequest;
import flipnote.user.interfaces.http.dto.request.LoginRequest;
import flipnote.user.interfaces.http.dto.request.PasswordResetCreateRequest;
import flipnote.user.interfaces.http.dto.request.PasswordResetRequest;
import flipnote.user.interfaces.http.dto.request.SignupRequest;
import flipnote.user.interfaces.http.dto.request.TokenValidateRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    @PostMapping("/register")
    public ResponseEntity<UserRegisterResult> register(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request.toCommand()));
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletResponse response
    ) {
        TokenPair tokenPair = authService.login(request.toCommand());
        setTokenCookies(response, tokenPair);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = HttpConstants.REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            HttpServletResponse response
    ) {
        authService.logout(refreshToken);
        CookieUtil.deleteCookie(response, HttpConstants.ACCESS_TOKEN_COOKIE);
        CookieUtil.deleteCookie(response, HttpConstants.REFRESH_TOKEN_COOKIE);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/token/refresh")
    public ResponseEntity<Void> refreshToken(
            @CookieValue(name = HttpConstants.REFRESH_TOKEN_COOKIE) String refreshToken,
            HttpServletResponse response
    ) {
        TokenPair tokenPair = authService.refreshToken(refreshToken);
        setTokenCookies(response, tokenPair);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/token/validate")
    public ResponseEntity<TokenValidateResult> validateToken(@Valid @RequestBody TokenValidateRequest request) {
        return ResponseEntity.ok(authService.validateToken(request.getToken()));
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader(HttpConstants.USER_ID_HEADER) Long userId,
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletResponse response
    ) {
        authService.changePassword(userId, request.toCommand());
        CookieUtil.deleteCookie(response, HttpConstants.ACCESS_TOKEN_COOKIE);
        CookieUtil.deleteCookie(response, HttpConstants.REFRESH_TOKEN_COOKIE);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email-verification/request")
    public ResponseEntity<Void> sendEmailVerification(@Valid @RequestBody EmailVerificationRequest request) {
        authService.sendEmailVerificationCode(request.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/email-verification")
    public ResponseEntity<Void> verifyEmail(@Valid @RequestBody EmailVerifyRequest request) {
        authService.verifyEmail(request.getEmail(), request.getCode());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<Void> requestPasswordReset(@Valid @RequestBody PasswordResetCreateRequest request) {
        authService.requestPasswordReset(request.getEmail());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/password-reset")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletResponse response
    ) {
        authService.resetPassword(request.getToken(), request.getPassword());
        CookieUtil.deleteCookie(response, HttpConstants.ACCESS_TOKEN_COOKIE);
        CookieUtil.deleteCookie(response, HttpConstants.REFRESH_TOKEN_COOKIE);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/social-links")
    public ResponseEntity<SocialLinksResult> getSocialLinks(
            @RequestHeader(HttpConstants.USER_ID_HEADER) Long userId) {
        return ResponseEntity.ok(authService.getSocialLinks(userId));
    }

    @DeleteMapping("/social-links/{socialLinkId}")
    public ResponseEntity<Void> deleteSocialLink(
            @RequestHeader(HttpConstants.USER_ID_HEADER) Long userId,
            @PathVariable Long socialLinkId
    ) {
        authService.deleteSocialLink(userId, socialLinkId);
        return ResponseEntity.noContent().build();
    }

    private void setTokenCookies(HttpServletResponse response, TokenPair tokenPair) {
        CookieUtil.addCookie(response, HttpConstants.ACCESS_TOKEN_COOKIE, tokenPair.accessToken(),
                jwtProvider.getAccessTokenExpiration() / 1000);
        CookieUtil.addCookie(response, HttpConstants.REFRESH_TOKEN_COOKIE, tokenPair.refreshToken(),
                jwtProvider.getRefreshTokenExpiration() / 1000);
    }
}
