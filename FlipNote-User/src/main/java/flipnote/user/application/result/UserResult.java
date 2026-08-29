package flipnote.user.application.result;

import flipnote.user.domain.entity.User;

public record UserResult(Long id, String email, String nickname, String profileImageUrl, String role) {

    public static UserResult from(User user) {
        return new UserResult(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl() != null ? user.getProfileImageUrl() : "",
                user.getRole().name()
        );
    }
}
