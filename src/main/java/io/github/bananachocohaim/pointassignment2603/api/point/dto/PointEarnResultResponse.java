package io.github.bananachocohaim.pointassignment2603.api.point.dto;

import java.time.LocalDate;

public record PointEarnResultResponse(
    String walletId,        // 고객 지갑 ID
    String earnId,          // 적립 ID 추적 및 취소용도
    Long earnedAmount,      // 실제 적립된 금액
    Long balance,    // 적립 후 해당 지갑의 최종 잔액
    LocalDate expirationDate // 적립된 포인트의 만료 예정일
) {
    public static PointEarnResultResponse of(String walletId, String earnId, Long earnedAmount, Long balance, LocalDate expirationDate) {
        return new PointEarnResultResponse(walletId, earnId, earnedAmount, balance, expirationDate);
    }
}
