package flipnote.user.application.result;

import flipnote.user.domain.entity.User;

public record UserRegisterResult(Long userId) {

    public static UserRegisterResult from(User user) {
        return new UserRegisterResult(user.getId());
    }
}
