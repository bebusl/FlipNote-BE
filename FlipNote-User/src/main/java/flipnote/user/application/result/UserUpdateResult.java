package flipnote.user.application.result;

import flipnote.user.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserUpdateResult {

    private Long userId;
    private String nickname;
    private String phone;
    private Boolean smsAgree;
    private String profileImageUrl;
    private Long imageRefId;

    public static UserUpdateResult from(User user, Long imageRefId) {
        return new UserUpdateResult(
                user.getId(),
                user.getNickname(),
                user.getPhone(),
                user.isSmsAgree(),
                user.getProfileImageUrl(),
                imageRefId
        );
    }
}
