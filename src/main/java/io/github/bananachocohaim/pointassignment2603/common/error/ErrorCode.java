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
    HANDLE_ACCESS_DENIED(403, "C006", "접근 권한이 없습니다."),
    WALLET_NOT_FOUND(404, "P001", "포인트 지갑을 찾을 수 없습니다."),
    BALANCE_NOT_ENOUGH(400, "P002", "잔액이 부족합니다."),
    BALANCE_LIMIT_EXCEEDED(400, "P003", "보유 한도를 초과합니다."),
    DUPLICATE_EARN_RECORD(409, "P004", "이미 존재하는 적립 건입니다."),
    DUPLICATE_USAGE_RECORD(409, "P005", "이미 존재하는 사용 건입니다."),
    EARN_NOT_FOUND(404, "P006", "적립 이력을 찾을 수 없습니다."),
    USAGE_NOT_FOUND(404, "P007", "사용 이력을 찾을 수 없습니다."),
    USAGE_ALREADY_CANCELLED(400, "P008", "이미 전체 취소된 거래입니다."),
    FULL_CANCEL_NOT_ALLOWED(400, "P009", "부분 취소 이력이 있어 전체 취소가 불가합니다."),
    CANCEL_AMOUNT_EXCEEDED(400, "P010", "취소 금액이 취소 가능 잔액을 초과합니다."),
    EARN_RECORD_RESTORE_FAILED(500, "P011", "적립 잔액 환급 처리에 실패했습니다. 동시 요청으로 인한 잔액 초과가 의심됩니다."),
    EARN_ALREADY_CANCELLED(400, "P012", "이미 취소된 적립 건입니다."),
    EARN_PARTIALLY_USED(400, "P013", "적립 금액 중 일부가 사용되어 적립 취소가 불가합니다."),
    POLICY_NOT_FOUND(404, "P014", "정책 정보를 찾을 수 없습니다."),
    EARN_AMOUNT_EXCEEDED(400, "P015", "1회 최대 적립 가능 금액을 초과합니다."),
    WALLET_LIMIT_UPDATE_NOT_ALLOWED(400, "P016", "FREE 타입 지갑만 보유 한도를 변경할 수 있습니다."),
    CANCEL_AMOUNT_REQUIRED(400, "P017", "부분 취소 시 취소 금액은 필수이며 1원 이상이어야 합니다."),
    ;

    private final int status;
    private final String errorCode;
    private final String errorMessage;

    ErrorCode(final int status, final String errorCode, final String errorMessage) {
        this.status = status;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }
}
