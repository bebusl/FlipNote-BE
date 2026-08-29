package flipnote.user.domain;

public record TokenClaims(
        Long userId,
        String email,
        String role
) {
}
