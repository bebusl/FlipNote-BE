package flipnote.user.interfaces.http.common;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public final class HttpConstants {
    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String ACCESS_TOKEN_COOKIE = "accessToken";
    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    public static final String OAUTH_VERIFIER_COOKIE = "oauth2_auth_request";
}
