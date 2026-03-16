package io.github.bananachocohaim.pointassignment2603.api.point.dto;

import io.github.bananachocohaim.pointassignment2603.domain.point.entity.UsageType;

public record PointUsageCancelResponse(
    String walletId,
    String cancelId,          //취소ID
    String originalUsageId,   //원거래 ID
    String originalOrderNo,   //원거래 주문번호
    Long cancelledAmount,    //취소 금액
    Long remainingUsageAmount, //남은 사용금액
    Long userPointWalletBalance, //남은 지갑 잔액
    UsageType usageType      // 취소 타입 PARTIAL_CANCEL, FULL_CANCEL
) { 
    public static PointUsageCancelResponse of(
        String walletId, String cancelId, String originalUsageId,
        String originalOrderNo, Long cancelledAmount, Long remainingUsageAmount, Long userPointWalletBalance, UsageType usageType
    ) {
        return new PointUsageCancelResponse(
            walletId, cancelId, originalUsageId, originalOrderNo, cancelledAmount, remainingUsageAmount, userPointWalletBalance, usageType);
    }
}
