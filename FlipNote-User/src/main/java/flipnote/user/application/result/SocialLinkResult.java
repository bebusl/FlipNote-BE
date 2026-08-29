package flipnote.user.application.result;

import flipnote.user.domain.entity.OAuthLink;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SocialLinkResult {

    private Long socialLinkId;
    private String provider;
    private LocalDateTime linkedAt;

    public static SocialLinkResult from(OAuthLink link) {
        return new SocialLinkResult(link.getId(), link.getProvider(), link.getLinkedAt());
    }
}
