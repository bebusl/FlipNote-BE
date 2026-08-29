package flipnote.user.domain;

import org.springframework.http.HttpStatus;

import flipnote.user.domain.common.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_001", "이메일 또는 비밀번호가 올바르지 않습니다."),
	ALREADY_ISSUED_VERIFICATION_CODE(HttpStatus.TOO_MANY_REQUESTS, "AUTH_002", "이미 인증코드가 발송되었습니다. 잠시 후 다시 시도해 주세요."),
	NOT_ISSUED_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "AUTH_003", "인증코드가 발송되지 않았습니다."),
	INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "AUTH_004", "인증코드가 올바르지 않습니다."),
	EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "AUTH_005", "이미 사용 중인 이메일입니다."),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_006", "유효하지 않은 토큰입니다."),
	UNVERIFIED_EMAIL(HttpStatus.FORBIDDEN, "AUTH_007", "이메일 인증이 완료되지 않았습니다."),
	ALREADY_SENT_PASSWORD_RESET_LINK(HttpStatus.TOO_MANY_REQUESTS, "AUTH_008", "이미 비밀번호 재설정 링크가 발송되었습니다. 잠시 후 다시 시도해 주세요."),
	INVALID_PASSWORD_RESET_TOKEN(HttpStatus.BAD_REQUEST, "AUTH_009", "유효하지 않은 비밀번호 재설정 토큰입니다."),
	INVALID_SOCIAL_LINK_TOKEN(HttpStatus.BAD_REQUEST, "AUTH_010", "유효하지 않은 소셜 연동 토큰입니다."),
	ALREADY_LINKED_SOCIAL_ACCOUNT(HttpStatus.CONFLICT, "AUTH_011", "이미 연결된 소셜 계정입니다."),
	NOT_REGISTERED_SOCIAL_ACCOUNT(HttpStatus.NOT_FOUND, "AUTH_012", "연결된 소셜 계정이 없습니다."),
	INVALID_OAUTH_PROVIDER(HttpStatus.BAD_REQUEST, "AUTH_013", "지원하지 않는 OAuth 제공자입니다."),
	OAUTH_COMMUNICATION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "AUTH_014", "OAuth 서버와의 통신 중 오류가 발생했습니다."),
	BLACKLISTED_TOKEN(HttpStatus.UNAUTHORIZED, "AUTH_015", "무효화된 토큰입니다."),
	INVALIDATED_SESSION(HttpStatus.UNAUTHORIZED, "AUTH_016", "세션이 무효화되었습니다. 다시 로그인해 주세요."),
	PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "AUTH_017", "현재 비밀번호가 일치하지 않습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;

	@Override
	public int getStatus() {
		return httpStatus.value();
	}
}
