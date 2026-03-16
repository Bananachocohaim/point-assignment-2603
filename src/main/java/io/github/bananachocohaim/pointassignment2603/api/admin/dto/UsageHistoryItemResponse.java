package io.github.bananachocohaim.pointassignment2603.api.admin.dto;

import java.time.LocalDate;
import java.util.List;

public record UsageHistoryItemResponse(
    String usageId,
    LocalDate createdDate,
    String usageType,
    String usageStatus,
    String orderNo,
    long usedAmount,
    String originalUsageId,         // 취소 건의 경우 원거래 usageId
    OriginalUsageInfo originalUsage, // 취소 건의 경우 원거래 상세
    List<EarnMappingDetail> earnMappings  // 차감/환급된 적립 건 목록
) {
    /** 원거래 요약 (취소 건에서 참조) */
    public record OriginalUsageInfo(
        String usageId,
        LocalDate createdDate,
        String usageType,
        String usageStatus,
        String orderNo,
        long usedAmount
    ) {}

    /** 사용-적립 매핑 상세 (적립 원장 정보 포함) */
    public record EarnMappingDetail(
        String earnId,
        String earnType,        // ORDER / MANUAL / EVENT / RE_EARN
        String earnStatus,      // ACTIVE / EXPIRED / CANCELLED
        long originalAmount,    // 적립 원금
        long remainingAmount,   // 현재 잔액
        long mappingAmount,     // 이 거래에서 차감/환급된 금액
        String mappingType      // USE / PARTIAL_CANCEL / FULL_CANCEL / EXPIRE
    ) {}
}
