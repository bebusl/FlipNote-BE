package flipnote.user.interfaces.http.common;

import flipnote.user.domain.common.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.springframework.validation.BindingResult;

import java.util.List;

@Getter
@Builder
public class ApiResponse<T> {

    private final int status;
    private final String code;
    private final String message;
    private final T data;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status(200)
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

    public static ApiResponse<List<FieldError>> validationError(BindingResult bindingResult) {
        return ApiResponse.<List<FieldError>>builder()
                .status(400)
                .code("INVALID_INPUT")
                .message("입력값이 올바르지 않습니다.")
                .data(FieldError.of(bindingResult))
                .build();
    }

    public static ApiResponse<Void> internalError() {
        return ApiResponse.<Void>builder()
                .status(500)
                .code("INTERNAL_SERVER_ERROR")
                .message("서버 오류가 발생했습니다.")
                .build();
    }

    @Getter
    @AllArgsConstructor
    public static class FieldError {
        private String field;
        private String rejectedValue;
        private String reason;

        public static List<FieldError> of(BindingResult bindingResult) {
            return bindingResult.getFieldErrors().stream()
                    .map(e -> new FieldError(
                            e.getField(),
                            e.getRejectedValue() == null ? "" : String.valueOf(e.getRejectedValue()),
                            e.getDefaultMessage()))
                    .toList();
        }
    }
}
