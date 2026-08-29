package flipnote.user.application.command;

public record UpdateProfileCommand(
        String nickname,
        String phone,
        Boolean smsAgree,
        Long imageRefId
) {}
