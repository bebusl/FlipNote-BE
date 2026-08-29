package flipnote.user.domain.common;

public interface ErrorCode {

    int getStatus();

    String getCode();

    String getMessage();
}
