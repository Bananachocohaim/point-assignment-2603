package io.github.bananachocohaim.pointassignment2603.api.point.dto;

public record PointEarnCancelResponse(
    String walletId,
    String earnId,
    Long cancelledAmount    // 취소된 적립 금액 (항상 원 적립 전체 금액)
) {
    public static PointEarnCancelResponse of(String walletId, String earnId, Long cancelledAmount) {
        return new PointEarnCancelResponse(walletId, earnId, cancelledAmount);
    }
}
