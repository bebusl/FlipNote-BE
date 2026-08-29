package flipnote.group.infrastructure.email;

public class EmailSendException extends RuntimeException {
	public EmailSendException(Throwable cause) {
		super(cause);
	}
}
