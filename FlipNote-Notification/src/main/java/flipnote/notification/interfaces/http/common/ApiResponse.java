package flipnote.notification.interfaces.http.common;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.validation.BindingResult;

import flipnote.notification.domain.common.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiResponse<T> {

	private final int status;
	private final String code;
	private final String message;
	private final T data;

	public static <T> ApiResponse<T> success(int status, T data) {
		return ApiResponse.<T>builder()
			.status(status)
			.data(data)
			.build();
	}

	public static ApiResponse<Void> error(ErrorCode errorCode) {
		return ApiResponse.<Void>builder()
			.status(errorCode.getStatus())
			.code(errorCode.getCode())
			.message(errorCode.getMessage())
			.build();
	}

	public static ApiResponse<List<FieldError>> error(ErrorCode errorCode, BindingResult bindingResult) {
		return ApiResponse.<List<FieldError>>builder()
			.status(errorCode.getStatus())
			.code(errorCode.getCode())
			.message(errorCode.getMessage())
			.data(FieldError.of(bindingResult))
			.build();
	}

	@Getter
	@AllArgsConstructor
	public static class FieldError {

		private String field;
		private String rejectedValue;
		private String reason;

		public static List<FieldError> of(BindingResult bindingResult) {
			return bindingResult.getFieldErrors()
				.stream()
				.map(error -> new FieldError(error.getField(),
					error.getRejectedValue() == null ? "" : error.getRejectedValue().toString(),
					error.getDefaultMessage()))
				.collect(Collectors.toList());
		}
	}
}
