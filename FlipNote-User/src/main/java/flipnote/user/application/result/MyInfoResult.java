package flipnote.user.application.result;

import flipnote.user.domain.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class MyInfoResult {

    private Long userId;
    private String email;
    private String nickname;
    private String name;
    private String phone;
    private Boolean smsAgree;
    private String profileImageUrl;
    private Long imageRefId;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    public static MyInfoResult from(User user) {
        return new MyInfoResult(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getName(),
                user.getPhone(),
                user.isSmsAgree(),
                user.getProfileImageUrl(),
                null,
                user.getCreatedAt(),
                user.getModifiedAt()
        );
    }
}
