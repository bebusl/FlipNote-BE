package flipnote.user.infrastructure.mail;

public interface MailService {

    void sendVerificationCode(String to, String code, int ttl);

    void sendPasswordResetLink(String to, String link, int ttl);
}
