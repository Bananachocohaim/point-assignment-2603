package io.github.bananachocohaim.pointassignment2603.api.point.dto;

import java.util.List;

import io.github.bananachocohaim.pointassignment2603.domain.point.entity.EarnStatus;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.EarnType;
import io.github.bananachocohaim.pointassignment2603.domain.point.entity.UsageType;

public record PointUsageResultResponse(
    String walletId,
    String usageId,
    String orderNo,
    Long usedAmount,
    Long balance,
    UsageType usageType,
    List<UsageEarnDetail> usageEarnDetails
) {
    public record UsageEarnDetail(
        EarnType earnType,
        String earnId,
        Long earnAmount,
        Long usedAmount,
        Long earnBalance,
        EarnStatus earnStatus
    ) {
      public static UsageEarnDetail of(EarnType earnType, String earnId, Long earnAmount, Long usedAmount, Long earnBalance, EarnStatus earnStatus) {
        return new UsageEarnDetail(earnType, earnId, earnAmount, usedAmount, earnBalance, earnStatus);
      } 
    }
    //사용 결과 응답 값 반환
    public static PointUsageResultResponse of(String walletId, String usageId, String orderNo, Long usedAmount, Long balance, UsageType usageType, List<UsageEarnDetail> usageEarnDetails) {
        return new PointUsageResultResponse(walletId, usageId, orderNo, usedAmount, balance, usageType, usageEarnDetails);
    }   
}
