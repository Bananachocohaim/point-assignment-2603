package io.github.bananachocohaim.pointassignment2603.api.admin.dto;

import java.time.LocalDate;

import io.github.bananachocohaim.pointassignment2603.domain.point.entity.PointEarnRecord;

public record EarnHistoryItemResponse(
    String earnId,
    LocalDate createdDate,
    String earnType,
    String earnStatus,
    String orderNo,
    long originalAmount,
    long remainingAmount,
    int expiryDays,
    LocalDate expirationDate,
    String originalEarnId       // RE_EARN인 경우 원본 적립 ID
) {
    public static EarnHistoryItemResponse from(PointEarnRecord r) {
        return new EarnHistoryItemResponse(
            r.getId().getEarnId(),
            r.getId().getCreatedDate(),
            r.getEarnType().name(),
            r.getEarnStatus().name(),
            r.getOrderNo(),
            r.getOriginalAmount(),
            r.getRemainingAmount(),
            r.getExpiryDays(),
            r.getExpirationDate(),
            r.getOriginalEarnId()
        );
    }
}
