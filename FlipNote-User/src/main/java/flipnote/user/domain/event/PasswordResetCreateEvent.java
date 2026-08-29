package flipnote.user.domain.event;

public record PasswordResetCreateEvent(
        String to,
        String link
) {
}
