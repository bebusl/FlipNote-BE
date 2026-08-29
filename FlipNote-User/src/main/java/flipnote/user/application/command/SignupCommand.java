package flipnote.user.application.command;

public record SignupCommand(
        String email,
        String password,
        String name,
        String nickname,
        String phone,
        boolean smsAgree
) {}
