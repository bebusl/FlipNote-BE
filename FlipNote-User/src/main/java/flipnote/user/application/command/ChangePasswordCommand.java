package flipnote.user.application.command;

public record ChangePasswordCommand(String currentPassword, String newPassword) {}
