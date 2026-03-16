package io.github.bananachocohaim.pointassignment2603.api.point.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record PointUsageCancelRequest(

    @NotBlank(message = "지갑 ID는 필수입니다.")
    String walletId,

    @NotBlank(message = "취소할 원거래 ID는 필수입니다.")
    String originalUsageId,

    // FULL_CANCEL 시 null 허용 (금액 자동 계산). PARTIAL_CANCEL 시에만 필수.
    @Positive(message = "취소 금액은 양수여야 합니다.")
    @Min(value = 1, message = "취소 금액은 최소 1원 이상이어야 합니다.")
    Long cancelAmount,

    @NotNull(message = "취소 타입은 필수입니다.")
    @Pattern(
        regexp = "PARTIAL_CANCEL|FULL_CANCEL", 
        message = "취소 타입은 PARTIAL_CANCEL(부분취소), FULL_CANCEL(전체취소)만 가능합니다."
    )
    String cancelType // 취소 타입 PARTIAL_CANCEL 부분취소, FULL_CANCEL 전체취소만 허용

) {}
