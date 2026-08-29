package flipnote.reaction.domain.common;

public interface ErrorCode {
	int getStatus();
	String getCode();
	String getMessage();
}
