package flipnote.user.application.result;

import flipnote.user.domain.entity.OAuthLink;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class SocialLinksResult {

    private List<SocialLinkResult> socialLinks;

    public static SocialLinksResult from(List<OAuthLink> links) {
        return new SocialLinksResult(links.stream().map(SocialLinkResult::from).toList());
    }
}
