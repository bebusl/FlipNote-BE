package flipnote.user.infrastructure.mail;

import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PasswordResetTokenGenerator {

    public String generate() {
        return UUID.randomUUID().toString();
    }
}
