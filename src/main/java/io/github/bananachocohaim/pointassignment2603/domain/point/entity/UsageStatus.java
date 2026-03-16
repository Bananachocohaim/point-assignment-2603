package io.github.bananachocohaim.pointassignment2603.domain.point.entity;

/**
 * 포인트 사용 거래 상태
 */
public enum UsageStatus {
    COMPLETED,       // 정상 완료 (취소 없음)
    PARTIAL_CANCEL,  // 부분 취소됨
    FULL_CANCEL      // 전체 취소됨
}
