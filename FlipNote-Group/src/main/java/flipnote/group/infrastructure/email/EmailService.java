package flipnote.group.infrastructure.email;

public interface EmailService {

	void sendGuestGroupInvitation(String to, String groupName, String registerUrl);
}
