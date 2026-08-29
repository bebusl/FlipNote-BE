package flipnote.notification.interfaces.http.common;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import flipnote.notification.domain.common.BizException;
import flipnote.notification.domain.common.CommonErrorCode;
import flipnote.notification.domain.common.ErrorCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BizException.class)
	public ResponseEntity<ApiResponse<Void>> handleBizError(BizException exception) {
		log.warn("BizException handled: code={}, status={}, message={}",
			exception.getErrorCode().getCode(),
			exception.getErrorCode().getStatus(),
			exception.getErrorCode().getMessage()
		);

		return ResponseEntity
			.status(exception.getErrorCode().getStatus())
			.body(ApiResponse.error(exception.getErrorCode()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiResponse<Void>> handleGeneralError(Exception exception) {
		log.error("Unhandled exception occurred", exception);

		ErrorCode errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR;
		return ResponseEntity
			.status(errorCode.getStatus())
			.body(ApiResponse.error(errorCode));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiResponse<List<ApiResponse.FieldError>>> handleValidationError(
		MethodArgumentNotValidException exception
	) {
		return ResponseEntity
			.badRequest()
			.body(ApiResponse.error(CommonErrorCode.INVALID_INPUT_VALUE, exception.getBindingResult()));
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameter(
		MissingServletRequestParameterException exception
	) {
		String missingParam = exception.getParameterName();
		String message = String.format("필수 파라미터 '%s'가 없습니다.", missingParam);
		return ResponseEntity
			.badRequest()
			.body(ApiResponse.<Void>builder()
				.status(400)
				.code(CommonErrorCode.INVALID_INPUT_VALUE.getCode())
				.message(message)
				.build());
	}
}
