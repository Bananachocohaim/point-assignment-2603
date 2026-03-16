package io.github.bananachocohaim.pointassignment2603.domain.point.entity;

/**
 * 포인트 사용/취소 기록의 행위 타입
 */
public enum UsageType {
    USE,            // 사용
    PARTIAL_CANCEL, // 부분 취소
    FULL_CANCEL     // 전체 취소
}
