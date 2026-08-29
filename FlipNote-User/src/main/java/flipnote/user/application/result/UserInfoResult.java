package flipnote.user.application.result;

import flipnote.user.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserInfoResult {

    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private Long imageRefId;

    public static UserInfoResult from(User user) {
        return new UserInfoResult(user.getId(), user.getNickname(), user.getProfileImageUrl(), null);
    }
}
