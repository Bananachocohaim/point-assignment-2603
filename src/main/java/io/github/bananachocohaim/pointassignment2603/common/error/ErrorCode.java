package io.github.bananachocohaim.pointassignment2603.common.error;

import lombok.Getter;

@Getter
public enum ErrorCode {
    // 공통 에러
    INVALID_INPUT_VALUE(400, "C001", "올바르지 않은 입력값입니다."),
    METHOD_NOT_ALLOWED(405, "C002", "지원하지 않는 HTTP 메서드입니다."),
    ENTITY_NOT_FOUND(400, "C003", "해당 엔티티를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(500, "C004", "서버 내부 오류가 발생했습니다."),
    INVALID_TYPE_VALUE(400, "C005", "요청 타입이 일치하지 않습니다."),
    HANDLE_ACCESS_DENIED(403, "C006", "접근 권한이 없습니다.");

    private final int status;
    private final String errorCode;
    private final String errorMessage;

    ErrorCode(final int status, final String errorCode, final String errorMessage) {
        this.status = status;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
