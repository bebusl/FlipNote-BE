package flipnote.user.infrastructure.mail;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class VerificationCodeGenerator {

    private static final SecureRandom RANDOM = new SecureRandom();

    public String generate() {
        int code = RANDOM.nextInt(1_000_000);
        return String.format("%06d", code);
    }
}
