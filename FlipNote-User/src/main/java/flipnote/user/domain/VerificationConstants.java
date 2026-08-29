package flipnote.user.domain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class VerificationConstants {

    public static final int CODE_TTL_MINUTES = 5;
    public static final int VERIFIED_TTL_MINUTES = 10;
}
