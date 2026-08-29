package flipnote.group.domain.policy;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Group
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "GROUP_001", "그룹을 찾을 수 없습니다"),
    GROUP_ALREADY_EXISTS(HttpStatus.CONFLICT, "GROUP_002", "이미 존재하는 그룹입니다"),
    GROUP_MEMBER_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "GROUP_003", "그룹 인원이 초과되었습니다"),
    GROUP_PRIVATE(HttpStatus.FORBIDDEN, "GROUP_004", "비공개 그룹입니다"),
    GROUP_INVALID_NAME(HttpStatus.BAD_REQUEST, "GROUP_005", "그룹 이름이 유효하지 않습니다"),
    GROUP_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "GROUP_006", "그룹 이름이 너무 깁니다"),
    GROUP_INVALID_MAX_MEMBER(HttpStatus.BAD_REQUEST, "GROUP_007", "최대 멤버 수가 유효하지 않습니다"),
    GROUP_INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "GROUP_008", "카테고리가 유효하지 않습니다"),
    GROUP_INVALID_JOIN_POLICY(HttpStatus.BAD_REQUEST, "GROUP_009", "가입 정책이 유효하지 않습니다"),
    GROUP_INVALID_VISIBILITY(HttpStatus.BAD_REQUEST, "GROUP_010", "공개 여부가 유효하지 않습니다"),
    GROUP_INVALID_DESCRIPTION(HttpStatus.BAD_REQUEST, "GROUP_011", "그룹 설명이 유효하지 않습니다"),

    // Member
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_001", "멤버를 찾을 수 없습니다"),
    MEMBER_ALREADY_JOINED(HttpStatus.CONFLICT, "MEMBER_002", "이미 가입된 멤버입니다"),
    USER_NOT_IN_GROUP(HttpStatus.NOT_FOUND, "MEMBER_003", "유저가 그룹의 멤버가 아닙니다"),
    MEMBER_COUNT_UNDERFLOW(HttpStatus.BAD_REQUEST, "MEMBER_004", "멤버 수가 0 미만이 될 수 없습니다"),

    // Permission
    PERMISSION_DENIED(HttpStatus.FORBIDDEN, "PERM_001", "권한이 없습니다"),
    PERMISSION_ALREADY_EXISTS(HttpStatus.CONFLICT, "PERM_002", "이미 존재하는 권한입니다"),
    PERMISSION_NOT_FOUND(HttpStatus.NOT_FOUND, "PERM_003", "권한을 찾을 수 없습니다"),
    PERMISSION_HOST_NOT_FOUND(HttpStatus.NOT_FOUND, "PERM_004", "호스트 권한이 존재하지 않습니다"),
    PERMISSION_ROLE_TOO_LOW(HttpStatus.FORBIDDEN, "PERM_005", "변경하려는 역할보다 권한이 낮습니다"),
    NOT_OWNER(HttpStatus.FORBIDDEN, "PERM_006", "그룹 오너가 아닙니다"),

    // Join
    JOIN_NOT_FOUND(HttpStatus.NOT_FOUND, "JOIN_001", "가입 신청을 찾을 수 없습니다"),
    JOIN_ALREADY_EXISTS(HttpStatus.CONFLICT, "JOIN_002", "이미 가입 신청이 존재합니다"),
    JOIN_ALREADY_ACCEPTED(HttpStatus.CONFLICT, "JOIN_003", "이미 수락된 가입 신청입니다"),
    JOIN_NOT_JOINABLE(HttpStatus.BAD_REQUEST, "JOIN_004", "가입이 불가능한 상태입니다"),

    // Image (gRPC)
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "IMAGE_001", "이미지를 찾을 수 없습니다"),
    IMAGE_INVALID_REQUEST(HttpStatus.BAD_REQUEST, "IMAGE_002", "잘못된 이미지 요청입니다"),
    IMAGE_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_003", "이미지 서버 내부 오류입니다"),
    IMAGE_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "IMAGE_004", "이미지 서비스 오류입니다"),

    // Invite
    INVITE_ALREADY_EXISTS(HttpStatus.CONFLICT, "INVITE_001", "이미 초대된 사용자입니다"),
    INVITE_PERMISSION_DENIED(HttpStatus.FORBIDDEN, "INVITE_002", "해당 그룹에 초대할 권한이 없습니다"),
    INVITE_NOT_FOUND(HttpStatus.NOT_FOUND, "INVITE_003", "유효하지 않은 초대입니다"),
    INVITE_SELF_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "INVITE_004", "본인을 초대할 수 없습니다"),

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_001", "잘못된 입력입니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_002", "서버 오류가 발생했습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
