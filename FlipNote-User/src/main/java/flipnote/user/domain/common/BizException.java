package flipnote.user.domain.common;

import flipnote.user.domain.common.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BizException extends RuntimeException {

	private ErrorCode errorCode;
}
