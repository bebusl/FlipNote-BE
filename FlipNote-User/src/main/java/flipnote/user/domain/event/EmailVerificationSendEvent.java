package flipnote.user.domain.event;

public record EmailVerificationSendEvent(
        String to,
        String code
) {
}
