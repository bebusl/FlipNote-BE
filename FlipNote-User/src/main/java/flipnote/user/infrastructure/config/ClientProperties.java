package flipnote.user.infrastructure.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.client")
public class ClientProperties {

    private final String url;
    private final Paths paths;

    @Getter
    @RequiredArgsConstructor
    public static class Paths {
        private final String passwordReset;
        private final String socialLinkSuccess;
        private final String socialLinkFailure;
        private final String socialLinkConflict;
        private final String socialLoginSuccess;
        private final String socialLoginFailure;
    }
}
